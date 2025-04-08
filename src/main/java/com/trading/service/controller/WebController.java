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
import com.trading.service.service.TradingService;

import reactor.core.publisher.Mono;

@Controller
public class WebController {

	@Autowired
	BinanceRestService restService;
	
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
				

	@ResponseBody
	@RequestMapping(value="/getTicker", method=RequestMethod.GET)
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
