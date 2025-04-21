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

import com.trading.service.model.Ticker;
import com.trading.service.model.Tickers;
import com.trading.service.service.BinanceRestService;
import com.trading.service.service.RedisService;
import com.trading.service.service.TradingService;

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
	
	@RequestMapping(value="/", method=RequestMethod.GET)
	public Mono<String> home(Model model) {
		return Mono.just("web/home");
	}
	
	@ResponseBody
	@RequestMapping(value="/getTicker", method=RequestMethod.GET)
	public Mono<Map<String, Object>> getTicker() {
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
	
	public Mono<Map<String, Object>> testGet(){
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
							.collectList()
							.map(resultTickers -> {
								Map<String, Object> map = new HashMap<>();
								map.put("tickers", resultTickers);
								return map;
							});
				})
		);
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
					// 1. 필수 심볼 따로 추출
					List<Ticker> tickers = ticker.stream()
					    .filter(t -> mustIncludeSymbols.contains(t.getSymbol()))
					    .collect(Collectors.toList());
					
					map.put("topTicker", topTicker);
					map.put("ticker", tickers);
					
					return Mono.just(map);
				})
		);
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
