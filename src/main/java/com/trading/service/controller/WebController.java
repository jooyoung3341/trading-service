package com.trading.service.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.trading.service.indicator.Indicator;
import com.trading.service.model.Candles;
import com.trading.service.model.Ticker;
import com.trading.service.service.BinanceRestService;
import com.trading.service.service.RedisService;
import com.trading.service.service.TradingService;

import jakarta.servlet.http.HttpServletRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Controller
public class WebController {

	@Autowired
	BinanceRestService restService;
	@Autowired
	RedisService redisService;
	@Autowired
	TradingService tradingService;
	@Autowired
	Indicator indicator;
	
	@RequestMapping(value="/", method=RequestMethod.GET)
	public Mono<String> home(Model model) {
		return Mono.just("web/home");
	}

	
	@ResponseBody
	@RequestMapping(value="/getTicker1", method=RequestMethod.GET)
	public Mono<Map<String, Object>> getTicker1() {
		return Mono.defer(() -> restService.getTickers()
				.flatMap(ticker -> {
					Map<String, Object> map = new HashMap<>();
					//24시간 가격변동 상위 코인 가져오기
					List<Ticker> topTicker = ticker.stream()
						    .sorted(Comparator.comparingDouble(data -> -Double.parseDouble(data.getPriceChangePercent())))
						    .limit(15)
						    .collect(Collectors.toList());
					Set<String> mustIncludeSymbols = Set.of("BTCUSDT", "ETHUSDT", "SOLUSDT");
					// 필수 코인 추출
					List<Ticker> tickers = ticker.stream()
					    .filter(t -> mustIncludeSymbols.contains(t.getSymbol()))
					    .collect(Collectors.toList());
					
					// 중복되지 않도록 topTicker에서 tickers에 없는 symbol만 필터링
					Set<String> existingSymbols = tickers.stream()
					    .map(Ticker::getSymbol)
					    .collect(Collectors.toSet());
					List<Ticker> tickerList = new ArrayList<>();
					tickerList.addAll(tickers); // tickers 먼저 추가
					tickerList.addAll(
					    topTicker.stream()
					             .filter(t -> !existingSymbols.contains(t.getSymbol())) // 중복 제거
					             .collect(Collectors.toList())
					);
					
					//List<Ticker> t = new Ticker();
					return Flux.fromIterable(tickerList)
				    .flatMap(ti -> tradingService.trand(ti.getSymbol(), "1m")
				            .map(tran -> {
				                ti.setM1_trand(tran);  // 여기서 안전하게 수정 가능
				                return ti;
				            })
				    )
				    .flatMap(ti -> tradingService.trand(ti.getSymbol(), "5m")
				    		.map(tran -> {
				    			ti.setM5_trand(tran);  // 여기서 안전하게 수정 가능
				                return ti;
				    		})
				    )
				    .flatMap(ti -> tradingService.trand(ti.getSymbol(), "15m")
				    		.map(tran -> {
				    			ti.setM15_trand(tran);  // 여기서 안전하게 수정 가능
				                return ti;
				    		})
				    )
				    .collectList()
				    .map(updatedTickers -> {
						map.put("tickerList", updatedTickers);
						return map;
					});
					//return Mono.just(map);
				})
		);
	}
	
	@ResponseBody
	@RequestMapping(value="/getTicke12r", method=RequestMethod.GET)
	public Mono<List<Ticker>> getTicker(){
		return Mono.defer(() -> redisService.getTradingSymbolList("TradingSymbol")
				.flatMap(symbolList -> {
					List<Ticker> tickers = new ArrayList<>();
					for (String s : symbolList) {
						Ticker t = new Ticker();
						t.setSymbol(s);
						tickers.add(t);
					}
					return Flux.fromIterable(tickers)
							.flatMap(ti -> tradingService.trand(ti.getSymbol(), "5m")
									.map(trand -> {
										ti.setM5_trand(trand);
										return ti;
									})
							)
							.collectList();
				})
		);
	}

	@ResponseBody
	@RequestMapping(value="/getTicker", method=RequestMethod.GET)
	public Mono<List<Ticker>> getTicker123(){
		return Mono.defer(() -> redisService.getTradingSymbolList("TradingSymbol")
				.flatMap(symbolList -> {
					List<Ticker> tickers = new ArrayList<>();
					for (String s : symbolList) {
						Ticker t = new Ticker();
						t.setSymbol(s);
						tickers.add(t);
					}
					return Flux.fromIterable(tickers)
							.flatMap(ti -> restService.getTicker(ti.getSymbol())
									.map(data -> {
										ti.setLastPrice(data.get(0).getLastPrice());
										ti.setPriceChangePercent(data.get(0).getPriceChangePercent());
										return ti;
									})
								)
							.flatMap(ti -> tradingService.trand(ti.getSymbol(), "1m")
									.map(trand -> {
										ti.setM1_trand(trand);
										return ti;
									})
								)
							.flatMap(ti -> tradingService.trand(ti.getSymbol(), "5m")
									.map(trand -> {
										ti.setM5_trand(trand);
										return ti;
									})
								)
							.flatMap(ti -> tradingService.trand(ti.getSymbol(), "15m")
									.map(trand -> {
										ti.setM15_trand(trand);
										return ti;
									})
								)
							.collectList();
				})
		);
	}
	@ResponseBody
	@RequestMapping(value="/addSymbol", method=RequestMethod.GET)
	public Mono<String> addSymbol(HttpServletRequest  request) {
		return Mono.defer(() -> redisService.targetTradingSymbol("TradingSymbol", request.getParameter("symbol"))
				.flatMap(targetResult -> {
					System.out.println("add symbol : " + request.getParameter("symbol"));
					System.out.println("targetTradingSymbol re  : " + targetResult);
					
					if(targetResult) {
						//중복
						return Mono.just("fail");
					}
					return redisService.addTradingSymbol("TradingSymbol", request.getParameter("symbol"))
							.flatMap(result -> {
								System.out.println("add re : " + result);
								return Mono.just("success");
							});
				})
			);
	}
	
	@ResponseBody
	@RequestMapping(value="/allTicket", method=RequestMethod.GET)
	public Mono<List<Ticker>> allTicket() {
		System.out.println("alltic");
		return Mono.defer(() -> restService.getTickers()
				.flatMap(ticker -> {
					System.out.println("allticket");
					
					return Mono.just(ticker);
				})
		);
	}
				
	@ResponseBody
	@RequestMapping(value="/symbolDetail", method=RequestMethod.GET)
	public Mono<Map<String, Object>> symbolDetail(HttpServletRequest  request) {
		return Mono.defer(() -> {
			String symbol = request.getParameter("symbol");
			return restService.getCandles(symbol, "5m", (99+11))
					.flatMap(m5_list -> restService.getCandles(symbol, "15m", (99+11))
							.flatMap(m15_list -> {
								Candles m5_candles = new Candles().setCandles(m5_list);
								List<Double> m5_close = m5_candles.getCloses().subList(0, (m5_candles.getCloses().size() -1));
								Candles m15_candles = new Candles().setCandles(m15_list);
								List<Double> m15_close = m15_candles.getCloses().subList(0, (m15_candles.getCloses().size() -1));
								double m5_ema25 = indicator.ema(m5_close, 25);
								double m5_ema99 = indicator.ema(m5_close, 99);
								double m15_ema25 = indicator.ema(m15_close, 25);
								double m15_ema99 = indicator.ema(m15_close, 99);
								Map<String, Object> map = new HashMap<>();
								//Math.floor(value * 10000) / 10000.0;
								map.put("m5_ema25", Math.floor(m5_ema25 * 100000) / 100000.0);
								map.put("m5_ema99", Math.floor(m5_ema99 * 100000) / 100000.0);
								map.put("m15_ema25", Math.floor(m15_ema25 * 100000) / 100000.0);
								map.put("m15_ema99", Math.floor(m15_ema99 * 100000) / 100000.0);
								return Mono.just(map);
							})
				);
		});
		
	}
	
	
		/*return Mono.zip(
				restService.getTicker24H("BTCUSDT"),
				restService.getTicker24H("ETHUSDT"),
				restService.getTicker24H("SOLUSDT")
				)
				.map(tuple -> {
					List<Ticker24h> r = new ArrayList<>();
					r.add((Ticker24h) tuple.getT1());
					r.add((Ticker24h) tuple.getT2());
					r.add((Ticker24h) tuple.getT3());
					return r;
				});*/
	
}
