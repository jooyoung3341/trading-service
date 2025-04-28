package com.trading.service.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.trading.service.common.Indicator;
import com.trading.service.common.TradingUtil;
import com.trading.service.model.Candle;
import com.trading.service.model.Candles;
import com.trading.service.model.EnumType;
import com.trading.service.model.QqeResult;

import reactor.core.publisher.Mono;

@Service
public class TradingService {
	@Autowired
	private BinanceRestService restService;
	@Autowired
	private Indicator indicator;
	@Autowired
	private TradingUtil util;
	
	
	//ema, ssl 로 추세확인
	public Mono<String> trandCandle(String symbol, String time) {
		int period9 = 9;
		int period25 = 25;
		int period99 = 99;
		return Mono.defer(() -> restService.getCandles(symbol, time, (99+11)))
				.flatMap(list -> {
							Candles candles = new Candles().setCandles(list);
							List<Double> close = candles.getCloses().subList(0, (candles.getCloses().size() -1));
												
							double ema99 = indicator.ema(close, period99);
							double ema25 = indicator.ema(close, period25);
							double ema9 = indicator.ema(close, period9);

							return trandStr1(ema99,ema25,ema9)
									.flatMap(trand -> {
										List<Double> sslData = indicator.ssl(candles.getHigh(), candles.getLow(), candles.getCloses(), 60);
										int sslData_size = (sslData.size()-1);
										List<Double> ssl = sslData.subList((sslData_size-10), sslData_size);
										
										for (int i = 0; i < (ssl.size()-1); i++) {
											if(trand.equals("none")) {
												return Mono.just(EnumType.None.name());
											}
											
											if(trand.equals(EnumType.Short.name())) {
												if(ssl.get(i) > ssl.get(i+1)) {
													//우하향중
													continue;
												}else {
													//ssl 우하향이 아니고 중간에 추세전환
													return Mono.just(EnumType.None.name());
												}
											}else {
												//long
												if(ssl.get(i) < ssl.get(i+1)) {
													//우상향중
													continue;
												}else {
													//ssl 우상향이 아니고 중간에 추세전환
													return Mono.just(EnumType.None.name());
												}
											}
										}
										return Mono.just(trand);
									});
				});
	}

	//ema, ssl 로 추세확인
	public Mono<String> trandType(String symbol, String time) {
		int period9 = 9;
		int period25 = 25;
		return Mono.defer(() -> restService.getCandles(symbol, time, (99+11)))
				.flatMap(list -> {
							Candles candles = new Candles().setCandles(list);
							List<Double> close = candles.getCloses().subList(0, (candles.getCloses().size() -1));
												
							double ema25 = indicator.ema(close, period25);
							double ema9 = indicator.ema(close, period9);

							return trandStr(ema25,ema9)
									.flatMap(trand -> {
										List<Double> sslData = indicator.ssl(candles.getHigh(), candles.getLow(), candles.getCloses(), 60);
										int sslData_size = (sslData.size()-1);
										List<Double> ssl = sslData.subList((sslData_size-10), sslData_size);
										
										for (int i = 0; i < (ssl.size()-5); i++) {
											if(trand.equals(EnumType.None.value())) {
												return Mono.just(EnumType.None.value());
											}
											
											if(trand.equals(EnumType.Short.value())) {
												if(ssl.get(i) > ssl.get(i+1)) {
													//우하향중
													continue;
												}else {
													//ssl 우하향이 아니고 중간에 추세전환
													return Mono.just(EnumType.None.value());
												}
											}else {
												//long
												if(ssl.get(i) < ssl.get(i+1)) {
													//우상향중
													continue;
												}else {
													//ssl 우상향이 아니고 중간에 추세전환
													return Mono.just(EnumType.None.value());
												}
											}
										}
										return Mono.just(trand);
									});
				});
	}

	//EMA 추세 확인
	public Mono<String> trandStr1(double ema99, double ema25, double ema9){
		return Mono.defer(() -> {
				if(ema99 > ema25 && ema25 > ema9) {
					return Mono.just(EnumType.Short.value());
				}else if(ema99 < ema25 && ema25 < ema9) {
					return Mono.just(EnumType.Long.value());
				}else {
					return Mono.just(EnumType.None.value());
				}
		});
	}
	
	//EMA 추세 확인
	public Mono<String> trandStr(double ema25, double ema9){
		return Mono.defer(() -> {
				if(ema25 > ema9) {
					return Mono.just(EnumType.Short.value());
				}else if(ema25 < ema9) {
					return Mono.just(EnumType.Long.value());
				}else {
					return Mono.just(EnumType.None.value());
				}
		});
	}
	
	public Mono<Double> emaPrice(String symbol, String time, int limit){
		return Mono.defer(() -> restService.getCandles(symbol, time, (limit+11))
				.flatMap(list -> {
					Candles candles = new Candles().setCandles(list);
					List<Double> close = candles.getCloses().subList(0, (candles.getCloses().size() -1));
					double ema = indicator.ema(close, limit);
					return Mono.just(ema);
				})
		);
	}
	

	
	//매물대
	public Mono<Map<Double, Double>> getVolumeProfile(String symbol, String time){
		return Mono.defer(() -> restService.getCandles(symbol, time, 121)
				.flatMap(list -> {					
					double minPrice = list.stream().mapToDouble(Candle::getMinPrice).min().orElse(0.0);
					double maxPrice = list.stream().mapToDouble(Candle::getMaxPrice).max().orElse(0.0);
					double priceRange = maxPrice - minPrice;
					
					  // 가격 범위가 너무 좁으면 zoneCount 줄이고 step도 최소값 보정
		            int zoneCount = 60;
		            double step = priceRange / zoneCount;
		            if (step <= 0) step = 0.0000001; // 초저가 코인 대응

					Map<Double, Double> volumeMap = new TreeMap<>();
					for (Candle c : list) {
						double low = c.getMinPrice();
						double high = c.getMaxPrice();
						double volume = c.getVolume();
						
						 for (double p = low; p <= high; p += step) {
							// 정밀 계산을 위해 BigDecimal 사용
							 BigDecimal bdStep = BigDecimal.valueOf(step);
		                     BigDecimal bdP = BigDecimal.valueOf(p);

		                     BigDecimal priceZone = bdP.divide(bdStep, 8, RoundingMode.FLOOR).multiply(bdStep);
		                     priceZone = priceZone.setScale(8, RoundingMode.HALF_UP); // 8자리 반올림

		                     volumeMap.merge(priceZone.doubleValue(), volume, Double::sum);
		                    }
					}
					
					return Mono.just(volumeMap);
				})
			);
	}
	
	public Mono<Double> getStrongestZone(String symbol, String time){
		return Mono.defer(() -> getVolumeProfile(symbol, time)
				.map(volMap -> volMap.entrySet().stream()
						.max(Map.Entry.comparingByValue())
						.map(Map.Entry::getKey)
						.orElse(0.0)						
						)
				);
	}
}
