package com.trading.service.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.trading.service.common.TradingUtil;
import com.trading.service.indicator.Indicator;
import com.trading.service.model.Candles;

import reactor.core.publisher.Mono;

@Service
public class TradingService {
	@Autowired
	private BinanceRestService restService;
	@Autowired
	private Indicator indicator;
	@Autowired
	private TradingUtil util;
	
	public Mono<String> trand(String symbol, String time) {
		int period9 = 9;
		int period25 = 25;
		int period99 = 99;
		return Mono.defer(() -> restService.getCandles(symbol, time, (99+11)))
				.flatMap(list -> {
							Candles candles = new Candles().setCandles(list);
							List<Double> close = candles.getCloses().subList(0, (candles.getCloses().size() -1));;
							
							int m5_close_size = close.size();
							
							double ema99 = indicator.ema(close, period99);
							double ema25 = indicator.ema(close, period25);
							double ema9 = indicator.ema(close, period9);

							return trandStr(ema99,ema25,ema9)
									.flatMap(trand -> {
										System.out.println("trand : " + trand);
										List<Double> sslData = indicator.ssl(candles.getHigh(), candles.getLow(), candles.getCloses(), 60);
										int sslData_size = (sslData.size()-1);
										List<Double> ssl = sslData.subList((sslData_size-10), sslData_size);

										for (int i = 0; i < (ssl.size()-1); i++) {
											if(trand.equals("none")) {
												return Mono.just("none");
											}
											
											if(trand.equals("short")) {
												if(ssl.get(i) > ssl.get(i+1)) {
													//우하향중
													continue;
												}else {
													//ssl 우하향이 아니고 중간에 추세전환
													return Mono.just("none");
												}
											}else {
												//long
												if(ssl.get(i) < ssl.get(i+1)) {
													//우상향중
													continue;
												}else {
													//ssl 우상향이 아니고 중간에 추세전환
													return Mono.just("none");
												}
											}
										}
										return Mono.just(trand);
									});
				});
	}
	
	public Mono<String> trandStr(double ema99, double ema25, double ema9){
		return Mono.defer(() -> {
				if(ema99 > ema25 && ema25 > ema9) {
					return Mono.just("short");
				}else if(ema99 < ema25 && ema25 < ema9) {
					return Mono.just("long");
				}else {
					return Mono.just("none");
				}
		});
	}
}
