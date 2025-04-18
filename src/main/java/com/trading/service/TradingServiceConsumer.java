package com.trading.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
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
import com.trading.service.model.QqeResult;
import com.trading.service.model.TradingData;
import com.trading.service.service.BinanceRestService;
import com.trading.service.service.TradingService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class TradingServiceConsumer implements CommandLineRunner{

	@Autowired
	private BinanceRestService restService;
	@Autowired
	private Indicator indicator;
	@Autowired
	private TradingUtil util;
	@Autowired
	private TradingService service;
	
	@Autowired
	private WebController t;
	
	int period9 = 9;
	int period25 = 25;
	int period99 = 99;
	
	@Override
	public void run(String... args) throws Exception {
		System.out.println("실행 됨 ");
		//long   = 롱
		//short  = 숏
		//none = 보합
		
		/* 해야 할거
		 * 15분봉 롱 숏 보합 구함, 3개심볼이 보합일경우 스킵
		 * 하나라도 롱 또는 숏일 경우 그 심볼만 가지고 폴링하기 여기까지 하자 
		 * 그리고 롱숏 어떻게 잡을지 정해보자고
		*/
		Flux.interval(Duration.ofSeconds(60))
		.flatMap(sec -> m15_symbol()
				.flatMap(m15List -> {
					if(m15List.size() < 1) {
						return Mono.empty();
					//여기에 5분봉?
					}
				})
				)
		
	}
	
	public Mono<List<TradingData>> m15_symbol(){
		Set<String> tradingSymbol = Set.of("BTCUSDT", "ETHUSDT", "SOLUSDT");
		List<TradingData> td = new TradingData().getTd();
		
		//일단 15분봉 롱 숏 부터 구하자
		return Mono.defer(() -> Flux.fromIterable(td)
					.flatMap(data -> service.trand(data.getSymbol(), "15m")
							.map(trand -> {
								data.setM15_trand(trand);
								return data;
							})
					).collectList()
					.flatMap(dataList -> {
						List<TradingData> rtd = new ArrayList<TradingData>();
						for (TradingData data : dataList) {
							if(!data.getM15_trand().equals("none")) {
								rtd.add(data);
							}
						}
						return Mono.just(rtd);
					})
		
				);
	}
	public Mono<Void> tasting1(){
		/*
		 * 1분봉, 5분봉 위주로 데이터를 구함.
		 * 단기추세는 5분봉으로 > 롱추세일 떄 롱을 익절 하고 바로 롱포지션 또 안잡기위해 1분봉 추세변환까지 대기, 5분봉 99선과 현재 가격 간격 확인
		 * 추세로 포지션을 잡을때는 1분봉 99선, 5분봉 25선 ( 롱일 떄는 1분봉이 99선보다 위에있을 떄 근처오거나 닿으면?? 
		 * 	qqe모드는 15분봉 기준으로 추세를 보는게 나을려나?
		 * 오히려 qqe 5분봉으로 구하고 -3~3 사이에는 포지션을 안잡게 하는게 더 나을지도?
		 * 24시간 기준
		 * 1분봉 데이터 가져오기 1,440개
		 * 5분봉 데이터 가져오기 288개
		 * ema9 < ema25 < ema99 상승 추세
		 * ema9 > ema25 > ema99 하락 추세
		 * 
		 * 오 방금 생각한건데
		 * ssl 3개 구해서 상승 하락 추세 구한뒤 포지션은 ema로 잡는다?
		 * qqe는 좀더 고민해봐야겠다
		 * 
		 * 
		*/
		AtomicInteger m1_ema_idx99 = new AtomicInteger(0);
		AtomicInteger m1_ema_idx25 = new AtomicInteger(0);
		AtomicInteger m1_ema_idx9 = new AtomicInteger(0);
	    AtomicInteger m5_ema_idx99 = new AtomicInteger(0);
	    AtomicInteger m5_ema_idx25 = new AtomicInteger(0);
	    AtomicInteger m5_ema_idx9 = new AtomicInteger(0);
	    
		String symbol = "BTCUSDT";
		int m1_candleCnt = 1440;
		int m5_candleCnt = 288;
		
		return Mono.defer(() -> restService.getCandles(symbol, "1m", m1_candleCnt)
					.flatMap(m1_list -> restService.getCandles(symbol, "5m", m5_candleCnt)
							.flatMap(m5_list -> restService.getPrice(symbol)
									.flatMap(price -> {

										Candles m1_candles = new Candles().setCandles(m1_list);
										Candles m5_candles = new Candles().setCandles(m5_list);
										List<Double> m1_ema99_list = new ArrayList<>(); // 1342
										List<Double> m1_ema25_list = new ArrayList<>(); // 1416
										List<Double> m1_ema9_list = new ArrayList<>(); // 1432
										
										for (int i = m1_candleCnt-1; i >= period99; i--) {
											m1_ema99_list.add(indicator.ema(m1_candles.getCloses().subList((i-period99), i), period99));
										}
										for (int i = m1_candleCnt-1; i >= period25; i--) {
											m1_ema25_list.add(indicator.ema(m1_candles.getCloses().subList((i-period25), i), period25));
										}
										for (int i = m1_candleCnt-1; i >= period9; i--) {
											m1_ema9_list.add(indicator.ema(m1_candles.getCloses().subList((i-period9), i), period9));
										}
										
										List<Double> m5_ema99_list = new ArrayList<>(); // 190
										List<Double> m5_ema25_list = new ArrayList<>(); // 264
										List<Double> m5_ema9_list = new ArrayList<>(); // 280
										
										for (int i = m5_candleCnt-1; i >= period99; i--) {
											m5_ema99_list.add(indicator.ema(m5_candles.getCloses().subList((i-period99), i), period99));
										}
										for (int i = m5_candleCnt-1; i >= period25; i--) {
											m5_ema25_list.add(indicator.ema(m5_candles.getCloses().subList((i-period25), i), period25));
										}
										for (int i = m5_candleCnt-1; i >= period9; i--) {
											m5_ema9_list.add(indicator.ema(m5_candles.getCloses().subList((i-period9), i), period9));
										}
										
										
										int qqe_idx = m5_candleCnt - m5_ema99_list.size();
										
										
										List<Double> m5_ssl = indicator.ssl(m5_candles.getHigh(), m5_candles.getLow(), m5_candles.getCloses(), 60);
										int idx = 1;
										for (double a : m5_ssl) {
											System.out.println(idx + " : " + a);
											idx++;
										}
										int m5_ssl_idx = m5_ssl.size() - m5_ema99_list.size();
										
										List<Double> qqe = new ArrayList<>();
										idx = 1;
										for (int i = m5_candleCnt-1; i >= 45; i--) {
	
											QqeResult primaryQqe = indicator.qqe(m5_candles.getCloses().subList((i-45), i), 12, 10, 6.0);
											qqe.add(primaryQqe.getSmoothedRsi());
											System.out.println(util.toKst(m5_candles.getOpenTime().get(i)) + " : primaryQqe.getSmoothedRsi() : " +idx + " : "+ (primaryQqe.getSmoothedRsi()-50));
											idx++;
										}
										
										
										//double m1_ema99 = indicator.ema(m1_candles.getCloses().subList(m1_ema_idx99.get(), 99), 99);
										//m1_ema_idx99.set(m1_ema_idx99.get()+99);
										
										/*double m1_ema25 = indicator.ema(m1_candles.getCloses(), 25);
										double m1_ema9 = indicator.ema(m1_candles.getCloses(), 9);
										
										double m5_ema99 = indicator.ema(m5_candles.getCloses(), 99);
										double m5_ema25 = indicator.ema(m5_candles.getCloses(), 25);
										double m5_ema9 = indicator.ema(m5_candles.getCloses(), 9);
										
										List<Double> m1_ssl = indicator.ssl(m1_candles.getHigh(), m1_candles.getLow(),
												m1_candles.getCloses(), 60);*/
										
										return Mono.empty();
									})
								)
						)
				);
	}
	


	public Mono<Void> tt(){
		return Mono.defer(() -> restService.getCandles("BTCUSDT", "5m", 10))
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
					System.out.println("ema : " + ema99);
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
					
					System.out.println("isBuySignal : " + isBuySignal);
					System.out.println("isSellSignal : " + isSellSignal);
					
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
