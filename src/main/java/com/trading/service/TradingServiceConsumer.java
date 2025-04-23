package com.trading.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import com.trading.service.common.TradingUtil;
import com.trading.service.controller.WebController;
import com.trading.service.indicator.Indicator;
import com.trading.service.model.Candle;
import com.trading.service.model.Candles;
import com.trading.service.model.EnumType;
import com.trading.service.model.QqeResult;
import com.trading.service.model.Ticker;
import com.trading.service.model.TradingData;
import com.trading.service.service.BinanceRestService;
import com.trading.service.service.RedisService;
import com.trading.service.service.TradingService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class TradingServiceConsumer implements CommandLineRunner{

	@Autowired
	private BinanceRestService restService;
	@Autowired
	private RedisService redisService;
	@Autowired
	private Indicator indicator;
	@Autowired
	private TradingUtil util;
	@Autowired
	private TradingService tradingService;
	@Autowired
	private WebController t;
	
	String key = "TradingSymbol";
	
	int period9 = 9;
	int period25 = 25;
	int period99 = 99;
	
	@Override
	public void run(String... args) throws Exception {
		System.out.println("실행 됨 ");
		//tt().subscribe();
		
		
		AtomicReference<Map<String, List<Double>>> closeSymbol = 
				new AtomicReference<>(new HashMap<>());
		
		List<TradingData> td = new ArrayList<>();
		TradingData t1 = new TradingData();
		TradingData t2 = new TradingData();
		TradingData t3 = new TradingData();
		t1.setSymbol("BTCUSDT");
		t2.setSymbol("ETHUSDT");
		t3.setSymbol("SOLUSDT");
		
		td.add(t1);
		td.add(t2);
		td.add(t3);
		//레디스에 리스트로 구할 심볼 넣기
		// 웹으로 조회, 추가, 삭제 하는거 넣기
		//하나만 조회하기 넣기 (그거는 테이블 이외에 오른쪽에 조그맣게 조회할수있게끔
		/*Flux.interval(Duration.ofSeconds(60))
		.flatMap(sec -> redisService.getTradingSymbolList(key)
				.flatMap(null)
				)*/
		//long   = 롱
		//short  = 숏
		//none = 보합
		
		/* 해야 할거
		 * 15분봉 롱 숏 보합 구함, 3개심볼이 보합일경우 스킵
		 * 하나라도 롱 또는 숏일 경우 그 심볼만 가지고 폴링하기 여기까지 하자 
		 * 그리고 롱숏 어떻게 잡을지 정해보자고
		*/
		
		/*
		 * 레디스에서 내 포지션 확인
			없을경우와 -> 있을경우
			없을 경우에는 바로 포지션 잡지 않기 위해 한번 꼬는 로직 추가
			
			포지션에 대한 정보 db에 저장
			
			포지션에 대한 시각화 웹으로 출력
		*/
		
		//포지션 on = true, off = false
		AtomicBoolean position = new AtomicBoolean(false);
		//처음 일 경우 신호가 오더라도 매매X
		AtomicBoolean isStart = new AtomicBoolean(true);
		//추세를 알기위한 15분
		AtomicReference<String> is15Trand = new AtomicReference<>();
		//추세를 알기위한 15분
		AtomicReference<String> is5Trand = new AtomicReference<>();
		AtomicInteger timeSeq = new AtomicInteger(0);

			

		
		/*Mono<Void> loop = Mono.defer(() -> {
		    if (!hasPosition.get()) {
		        return Flux.interval(Duration.ofSeconds(5)) // 포지션 있을 때
		                .flatMap(tick -> doWhenHasPosition()
		                        .flatMap(pos -> {
		                            if (!pos) {
		                                hasPosition.set(false); // 상태 변경
		                                return Mono.error(new RuntimeException("상태 변경")); // 중지
		                            }
		                            return Mono.empty(); // 계속 유지
		                        }))
		                .onErrorResume(e -> Mono.empty()) // 에러(상태 변경)시 종료
		                .then();
		    } else {
		        return Flux.interval(Duration.ofSeconds(60)) // 포지션 없을 때
		                .flatMap(tick -> doWhenNoPosition()
		                        .flatMap(pos -> {
		                            if (pos) {
		                                hasPosition.set(true); // 상태 변경
		                                return Mono.error(new RuntimeException("상태 변경")); // 중지
		                            }
		                            return Mono.empty();
		                        }))
		                .onErrorResume(e -> Mono.empty()) // 에러(상태 변경)시 종료
		                .then();
		    }
		}).repeat(); // 상태 전환되면 다시 시작*/


	}
	
	public Mono<Void> redisAutoTrading(AtomicBoolean position, AtomicBoolean isStart, AtomicReference<String> is15Trand, AtomicReference<String> is5Trand) {
	    return Mono.defer(() ->redisService.getValue("isAuto") // Mono<String>
	            .flatMap(isAuto -> {
	                if ("false".equals(isAuto)) {
	                    // 포지션 없을 때만 60초마다 반복
	                    return Flux.interval(Duration.ofSeconds(60))
	                            .flatMap(tick -> redisService.getTradingSymbolList("autoSymbol") // Mono<List<String>>
	                                    .flatMapMany(Flux::fromIterable) // List<String> → Flux<String>
	                                    .flatMap(symbol ->tradingService.trandCandle(symbol, "5m") // Mono<String>
	                                    		.flatMap(m5_trand -> tradingService.trandCandle(symbol, "15m")
	                                    				.flatMap(m15_trand -> {
	    	                                    			if(m15_trand.equals("none")) {
	    	                                    				//횡보면 리턴
	    	                                    				return Mono.empty();
	    	                                    			}
	    	                                    			
	    	                                    			if(!is15Trand.get().equals(m15_trand) && !is15Trand.get().equals(null)) {
	    	                                    				//추세가 전환되면 다시 처음부터
	    	                                    				is15Trand.set(null);
	    	                                    				is5Trand.set(null);
	    	                                    				return Mono.empty();
	    	                                    			}
	    	                                    			is5Trand.set(m5_trand);
	    	                                    			is15Trand.set(m15_trand);
	    	                                    			return Mono.just(symbol);
	                                    				})

	                                    		)
	                                    )
	                                    .next() // 조건에 맞는 symbol 하나만 처리하고 Flux 종료
	                                    .flatMap(symbol -> {
	                                        //System.out.println("📌 진입할 심볼: " + symbol);
	                                       // position.set(true); // 포지션 잡힘
	                                        return Mono.empty();  //tradingService.enterPosition(symbol); // Mono<Void>
	                                    })
	                                    .switchIfEmpty(Mono.fromRunnable(() -> {
	                                        System.out.println("⚠️ 조건에 맞는 심볼 없음");
	                                    }))
	                            )
	                            .then(); // Flux<Void> → Mono<Void>
	                } else {
	                    return Mono.empty(); // 포지션 있으면 아무것도 안함
	                }
	            })
	    );
	}
	//15분봉으로만 추세를 보고
	//5분봉 역추세일 경우 15분봉 ema에 진입?
	public Mono<Void> redisAutoPositionOpen(String symbol, AtomicBoolean isStart, AtomicReference<String> is15Trand, AtomicReference<String> is5Trand){
		return Mono.defer(() -> restService.getCandles(symbol, "5m", (99+11))
				.flatMap(m5_list -> restService.getCandles(symbol, "15m", (99+11))
						.flatMap(m15_list -> restService.getPrice(symbol)
								.flatMap(price -> {
									Candles m5_candles = new Candles().setCandles(m5_list);
									List<Double> m5_close = m5_candles.getCloses().subList(0, (m5_candles.getCloses().size() -1));
									Candles m15_candles = new Candles().setCandles(m15_list);
									List<Double> m15_close = m15_candles.getCloses().subList(0, (m15_candles.getCloses().size() -1));
									
									double m5_ema99 = indicator.ema(m5_close, 99);
									double m15_ema25 = indicator.ema(m15_close, 25);
									
									if(is15Trand.get().equals(EnumType.Long.name())) {
										//롱
										if(!is5Trand.get().equals(is15Trand)) {
											//반대일경우에만 포지션 진입
											if(m5_ema99 < price || m15_ema25 < price) {
												//진입
												
											}
										}
										
									}else {
										//숏
										if(!is5Trand.get().equals(is15Trand)) {
											//반대일경우에만 포지션 진입
											if(m5_ema99 > price || m15_ema25 > price) {
												//진입
												
											}
										}
										
									}
									return Mono.empty();
								})
						)

				)
		);
	}
	
	
	public Mono<Void> tt(){
		return Mono.defer(() -> restService.getCandles("BTCUSDT", "15m", 100))
				.flatMap(list -> {
					List<Double> closes = list.stream()
		                    .map(Candle::getClose)
		                    .toList();
					List<Double> high = list.stream()
		                    .map(Candle::getHigh)
		                    .toList();
					List<Double> low = list.stream()
		                    .map(Candle::getLow)
		                    .toList();
					double ema99 = indicator.ema(closes, 99);
					//System.out.println("ema : " + ema99);
					QqeResult primaryQqe = indicator.qqe(closes, 12, 10, 6.0);
					QqeResult secondQqe = indicator.qqe(closes, 12, 10, 1.61);
					System.out.println("primaryQqe trendLine : " + primaryQqe.getTrendLine());
					System.out.println("primaryQqe smoothed Line : " + primaryQqe.getSmoothedRsi());
					
					System.out.println("secondQqe trendLine : " + secondQqe.getTrendLine());
					System.out.println("secondQqe smoothed Line : " + secondQqe.getSmoothedRsi());
					double thresholdSecondary = 6.0;

					// RSI 강도 판단 (보조 지표)
					boolean secondaryUp = secondQqe.getSmoothedRsi() - 50 > thresholdSecondary;
					boolean secondaryDown = secondQqe.getSmoothedRsi() - 50 < -thresholdSecondary;
					System.out.println("secondaryUp : " + secondaryUp);
					System.out.println("secondaryDown : " + secondaryDown);
					// 볼린저 밴드 계산
					double basis = indicator.sma(closes, 50);
					double dev = indicator.stdDev(closes, 50);
					double bollingerUpper = basis + dev * 0.35;
					double bollingerLower = basis - dev * 0.35;
					
					// 추세 강도 판단 (메인 지표)
					boolean primaryUp = primaryQqe.getSmoothedRsi() - 50 > bollingerUpper;
					boolean primaryDown = primaryQqe.getSmoothedRsi() - 50 < bollingerLower;
					//primaryQqe.getSmoothedRsi() - 50  이게 막대지표 인듯
					System.out.println("primaryQqe.getSmoothedRsi() - 50 : " + (primaryQqe.getSmoothedRsi() - 50));
					System.out.println("secondQqe.getSmoothedRsi() - 50 : " + (secondQqe.getSmoothedRsi() - 50));
					
					System.out.println("bollingerUpper : " + bollingerUpper);
					System.out.println("bollingerLower : " + bollingerLower);
					// 최종 시그널
					boolean isBuySignal = secondaryUp && primaryUp;
					boolean isSellSignal = secondaryDown && primaryDown;
					
					//System.out.println("isBuySignal : " + isBuySignal);
					//System.out.println("isSellSignal : " + isSellSignal);
					
					List<Double> ssl = indicator.ssl(high, low, closes, 60);
					
					int i = 0;
					for (double s : ssl) {
						System.out.println(i + " / ssl : " + s);
						i++;
					}
					/*
					 * 1분봉, 5분봉 위주로 데이터를 구함.
					 * 단기추세는 5분봉으로 > 롱추세일 떄 롱을 익절 하고 바로 롱포지션 또 안잡기위해 1분봉 추세변환까지 대기, 5분봉 99선과 현재 가격 간격 확인
					 * 추세로 포지션을 잡을때는 1분봉 99선, 5분봉 25선 ( 롱일 떄는 1분봉이 99선보다 위에있을 떄 근처오거나 닿으면?? 
					 * 	qqe모드는 15분봉 기준으로 추세를 보는게 나을려나?
					 * 오히려 qqe 5분봉으로 구하고 -3~3 사이에는 포지션을 안잡게 하는게 더 나을지도?
					*/					
					
					return Mono.empty();
				});
	}
}
