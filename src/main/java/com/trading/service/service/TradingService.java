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
	
	public Mono<String> trand(String symbol) {
		int period9 = 9;
		int period25 = 25;
		int period99 = 99;
		return Mono.defer(() -> restService.getCandles(symbol, "5m", (99+11)))
				.flatMap(m5_list -> {
							Candles m5_candles = new Candles().setCandles(m5_list);
							List<Double> m5_close = m5_candles.getCloses().subList(0, (m5_candles.getCloses().size() -1));;
							
							int m5_close_size = m5_close.size();
							
							double m5_ema99 = indicator.ema(m5_close, period99);
							double m5_ema25 = indicator.ema(m5_close, period25);
							double m5_ema9 = indicator.ema(m5_close, period9);

							return trandStr(m5_ema99,m5_ema25,m5_ema9)
									.flatMap(trand -> {
										List<Double> m5_sslData = indicator.ssl(m5_candles.getHigh(), m5_candles.getLow(), m5_candles.getCloses(), 60);
										int m5_sslData_size = (m5_sslData.size()-1);
										List<Double> m5_ssl = m5_sslData.subList((m5_sslData_size-10), m5_sslData_size);

										for (int i = 0; i < m5_ssl.size(); i++) {
											if(trand.equals("short")) {
												if(m5_ssl.get(i) > m5_ssl.get(i+1)) {
													//우하향중
													continue;
												}else {
													//ssl 우하향이 아니고 중간에 추세전환
													return Mono.just("none");
												}
											}else {
												//long
												if(m5_ssl.get(i) < m5_ssl.get(i+1)) {
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
