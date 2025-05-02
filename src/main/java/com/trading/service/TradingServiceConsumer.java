package com.trading.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import com.trading.service.DB.History;
import com.trading.service.DB.HistoryService;
import com.trading.service.common.Indicator;
import com.trading.service.common.TradingUtil;
import com.trading.service.controller.WebController;
import com.trading.service.model.Candle;
import com.trading.service.model.Candles;
import com.trading.service.model.EnumType;
import com.trading.service.model.QqeResult;
import com.trading.service.model.Ticker;
import com.trading.service.service.BinanceRestService;
import com.trading.service.service.BinanceService;
import com.trading.service.service.RedisService;
import com.trading.service.service.TeleService;
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
	private BinanceService binanceService;
	@Autowired
	private Indicator indicator;
	@Autowired
	private TradingUtil util;
	@Autowired
	private TradingService tradingService;
	@Autowired
	private HistoryService historyService;
	@Autowired
	private WebController t;
	@Autowired
	private TeleService teleService;

	
	@Override
	public void run(String... args) throws Exception {
		System.out.println("실행 됨 ");

		//long   = 롱
		//short  = 숏
		//none = 보합
		String tradingType = EnumType.TradingSymbol.value();
		String tel15 = EnumType.m15_tele.value();
		String tel5 = EnumType.m5_tele.value();
		String tel1 = EnumType.m1_tele.value();
		
		//false = 포지션 X / true = 포지션O
		AtomicBoolean isPosition = new AtomicBoolean(false);
		
	//	m15Process(tradingType, tel15, isPosition);
	//	m5Process(tradingType, tel5, isPosition);
	//	m1Process(tradingType, tel15, tel5, tel1, isPosition);
	}
	/*
	 * stc, 정배여부 두개돌린다?
	 * 포지션 진입 기준(롱기준) : 15분봉 ema 9 25 99 정배열, stc 초록색 일 경우 5분봉 ema9, 25 정배 stc 빨강 -> 이후 1분봉 빨강 이였다가 초록 될 떄 진입
	 * 포지션 종료 기준(롱기준) : price가 5분 ema20아래, ssl 역추세 아래, stc 역추세 빨강 => 이건 좀 생각해보자
	 * 										포지션종료 로직은 10초마다
	 * 
	*/
	
	public void m15Process(String listType, String processType, AtomicBoolean isPosition) {
		System.out.println("m15Process start");
		Flux.interval(Duration.ofMinutes(10))
			.startWith(0L)		
			.flatMap(m15 ->{
				if(!isPosition.get()) {
					return redisService.getTradingSymbolList(listType)
					.flatMapMany(Flux::fromIterable)
					.flatMap(symbol -> restService.getPrice(symbol)
						.flatMap(price -> restService.getCandles(symbol, EnumType.m15.value(), Integer.parseInt(EnumType.candle.value()))
							.flatMap(list -> {
											Candles candles = new Candles().setCandles(list);
											List<Double> close = candles.getCloses().subList(0, (candles.getCloses().size() -1));
											double ema25 = indicator.ema(close, 25);
											double ema9 = indicator.ema(close, 9);
											double ema99 = indicator.ema(close, 99);
											
											return indicator.getSTC(close, 88, 52, 63)
													.flatMap(stc -> {
														String trand = "";
														if(ema99 > ema25 && ema25 > ema9 && stc.equals(EnumType.Short.value())) {
															trand = EnumType.Short.value();
														}else if(ema99 < ema25 && ema25 < ema9 && stc.equals(EnumType.Long.value())) {
															trand = EnumType.Long.value();
														}else {
															trand = EnumType.None.value();
														}
														System.out.println("[m15Process] m15 추세 : " + trand);
														return redisService.saveValue(processType+symbol, trand)
																.then();
													});
									})
								)
							);
				}else {
					return Mono.empty();
				}
					
	}).subscribe();
	}
	
	public void m5Process(String listType, String processType, AtomicBoolean isPosition) {
		System.out.println("m5Process start");
		Flux.interval(Duration.ofMinutes(3))
			.startWith(0L)
			.flatMap(m5 -> {
				if(!isPosition.get()) {
					return redisService.getTradingSymbolList(listType)
					.flatMapMany(Flux::fromIterable)
						.flatMap(symbol -> redisService.getValue(processType+symbol)
								.flatMap(trand -> {
									return restService.getPrice(symbol)
											.flatMap(price -> restService.getCandles(symbol, EnumType.m5.value(), Integer.parseInt(EnumType.candle.value()))
												.flatMap(list -> {
													Candles candles = new Candles().setCandles(list);
													List<Double> close = candles.getCloses().subList(0, (candles.getCloses().size() -1));
													double ema25 = indicator.ema(close, 25);
													//double ema9 = indicator.ema(close, 9);
													
													double ssl = indicator.sslLast(candles.getHigh(), candles.getLow(), candles.getCloses(), 70);
													
													return indicator.getSTC(close, 88, 52, 63)
															.flatMap(stc -> {
																String m5_trand = "";
																if(ema25 < ssl && stc.equals(EnumType.Short.value())) {
																	m5_trand = EnumType.Short.value();
																}else if(ema25 > ssl&& stc.equals(EnumType.Long.value())) {
																	m5_trand = EnumType.Long.value();
																}else {
																	m5_trand = EnumType.None.value();
																}
																System.out.println("[m5Process] m5 추세 : " + m5_trand);
																return redisService.saveValue(processType+symbol, m5_trand)
																		.then();
															});
												})	
													
											);
								})
							);
				} else {
					return Mono.empty();
				}
	}).subscribe();
	}
	
	//m1_position : false - 포지션X 첫번쨰 / true - 포지션 O 두번쨰
	public void m1Process(String listType, String m15Type, String m5Type, String m1Type, AtomicBoolean isPosition) {
		System.out.println("m1Process start");
		Flux.interval(Duration.ofSeconds(50))
			.startWith(0L)
			.flatMap(m1 -> redisService.getTradingSymbolList(listType)
						.flatMapMany(Flux::fromIterable)
						.flatMap(symbol -> redisService.getValue(m15Type+symbol)
							.flatMap(m15_trand -> redisService.getValue(m5Type+symbol)
								.flatMap(m5_trand -> redisService.getValue(m1Type+symbol)
										.flatMap(m1_position -> {
													//5 15 추세 다르거나 								 하나라도 none일 경우 리턴
												if(!m5_trand.equals(m15_trand) || (m15_trand.equals(EnumType.None.value()) || m5_trand.equals(EnumType.None.value()))) {
													System.out.println("[m1Process] 5m 15m 추세 다르거나 None : ");
													System.out.println("[m1Process] 5m 추세 : " + m5_trand);
													System.out.println("[m1Process] 15m 추세 : " + m15_trand);
													return redisService.saveValue(m1Type+symbol, "false")
															.then();
												}
												System.out.println("[m1Process] 추세 통과 : " + m15_trand);
												return restService.getCandles(symbol, EnumType.m1.value(), Integer.parseInt(EnumType.candle.value()))
														.flatMap(list -> {
															Candles candles = new Candles().setCandles(list);
															List<Double> close = candles.getCloses().subList(0, (candles.getCloses().size() -1));
															return indicator.getSTC(close, 88, 52, 63)
																	.flatMap(stc -> { 
																		System.out.println("[m1Process] position : " + m1_position);
																		if(m5_trand.equals(stc) && m15_trand.equals(stc)) {
																			//같은추세
																			if(!m1_position.equals(EnumType.redisFalse.value())) {
																				//첫 추세면 매매 X
																				return Mono.empty();
																			}else if(m1_position.equals(EnumType.redisTrue.value())) {
																				//매매
																				System.out.println("[m1Process] 포지션 진입");
																				//isPosition.set(true);
																				return restService.getPrice(symbol)
																						.flatMap(price -> {
																							String teleText = symbol+" : " + stc + " 추세 진입! 진입가격 : " + price;
																							return teleService.sendMessage(teleText)
																									.flatMap(r -> redisService.saveValue(m1Type+symbol, EnumType.redisFalse.value())).then();
																									
																						});
																			}
																		}else {
																			//반대추세
																			if(!m1_position.equals(EnumType.redisFalse.value())) {
																				return redisService.saveValue(m1Type+symbol, EnumType.redisTrue.value())
																						.then();
																			}
																		}
																		return Mono.empty();
																	});
														});
										})
									)
								)
						)
			).subscribe();
	}

	
	public String teleStr(String asisTrand, String toTrand, String symbol, String price, String time) {
		return symbol + " " + time + " 추세전환 ! " + asisTrand + " -> " + toTrand + " 현재 가격 : " + price;
	}
	public Mono<Boolean> saveTele(String symbol, String m5_trand, String m15_trand){
		return Mono.defer(() -> redisService.saveValue(EnumType.m15_tele.value()+symbol, m15_trand)
				.flatMap(r -> redisService.saveValue(EnumType.m5_tele.value()+symbol, m5_trand)
						.flatMap(re -> {
							return Mono.just(re);
						})
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
