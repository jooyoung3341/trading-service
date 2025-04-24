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

import com.trading.service.common.Indicator;
import com.trading.service.model.Candles;
import com.trading.service.model.EnumType;
import com.trading.service.model.QqeResult;
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
	@RequestMapping(value="/getTicker", method=RequestMethod.GET)
	public Mono<List<Ticker>> getTicker(){
		return Mono.defer(() -> redisService.getTradingSymbolList(EnumType.TradingSymbol.value())
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
							.flatMap(ti -> tradingService.trandCandle(ti.getSymbol(), EnumType.m1.value())
									.map(trand -> {
										ti.setM1_trand(trand);
										return ti;
									})
								)
							.flatMap(ti -> tradingService.trandCandle(ti.getSymbol(), EnumType.m5.value())
									.map(trand -> {
										ti.setM5_trand(trand);
										return ti;
									})
								)
							.flatMap(ti -> tradingService.trandCandle(ti.getSymbol(), EnumType.m15.value())
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
	@RequestMapping(value="/getTopVolum", method=RequestMethod.GET)
	public Mono<List<Ticker>> getTopVolum(){
		return Mono.defer(() -> restService.getTopVolum()
				.flatMap(topList -> {
					return Flux.fromIterable(topList)
							.flatMap(ti -> tradingService.trandCandle(ti.getSymbol(), EnumType.m1.value())
									.map(trand -> {
										ti.setM1_trand(trand);
										return ti;
									})
								)
							.flatMap(ti -> tradingService.trandCandle(ti.getSymbol(), EnumType.m5.value())
									.map(trand -> {
										ti.setM5_trand(trand);
										return ti;
									})
								)
							.flatMap(ti -> tradingService.trandCandle(ti.getSymbol(), EnumType.m15.value())
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
	@RequestMapping(value="/getTopPercent", method=RequestMethod.GET)
	public Mono<List<Ticker>> getTopPercent(){
		return Mono.defer(() -> restService.getTopPercent()
				.flatMap(topList -> {
					return Flux.fromIterable(topList)
							.flatMap(ti -> tradingService.trandCandle(ti.getSymbol(), EnumType.m1.value())
									.map(trand -> {
										ti.setM1_trand(trand);
										return ti;
									})
								)
							.flatMap(ti -> tradingService.trandCandle(ti.getSymbol(), EnumType.m5.value())
									.map(trand -> {
										ti.setM5_trand(trand);
										return ti;
									})
								)
							.flatMap(ti -> tradingService.trandCandle(ti.getSymbol(), EnumType.m15.value())
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
					if(targetResult) {
						//중복
						return Mono.just(EnumType.Fail.value());
					}
					return redisService.addTradingSymbol("TradingSymbol", request.getParameter("symbol"))
							.flatMap(result -> {
								return Mono.just(EnumType.Success.value());
							});
				})
			);
	}
	
	@ResponseBody
	@RequestMapping(value="/deleteSymbol", method=RequestMethod.GET)
	public Mono<Boolean> deleteSymbol(HttpServletRequest  request) {
		return Mono.defer(() -> redisService.removeTradingSymbol("TradingSymbol", request.getParameter("symbol"))
				.flatMap(remove -> {
					return Mono.just(true);
				})
			);
	}
	
	@ResponseBody
	@RequestMapping(value="/allTicket", method=RequestMethod.GET)
	public Mono<List<Ticker>> allTicket() {
		System.out.println("alltic");
		return Mono.defer(() -> restService.getTickers()
				.flatMap(ticker -> {					
					return Mono.just(ticker);
				})
		);
	}
				
	@ResponseBody
	@RequestMapping(value="/symbolDetail", method=RequestMethod.GET)
	public Mono<Map<String, Object>> symbolDetail(HttpServletRequest  request) {
		return Mono.defer(() -> {
			String symbol = request.getParameter("symbol");
			return restService.getCandles(symbol, EnumType.m5.value(), (99+11))
					.flatMap(m5_list -> restService.getCandles(symbol, EnumType.m15.value(), (99+11))
							.flatMap(m15_list -> restService.getPrice(symbol)
									.flatMap(price -> {
										Candles m5_candles = new Candles().setCandles(m5_list);
										List<Double> m5_close = m5_candles.getCloses().subList(0, (m5_candles.getCloses().size() -1));
										Candles m15_candles = new Candles().setCandles(m15_list);
										List<Double> m15_close = m15_candles.getCloses().subList(0, (m15_candles.getCloses().size() -1));
										double m5_ema25 = indicator.ema(m5_close, 25);
										double m5_ema99 = indicator.ema(m5_close, 99);
										double m15_ema25 = indicator.ema(m15_close, 25);
										double m15_ema99 = indicator.ema(m15_close, 99);
										
										QqeResult m5_qqe = indicator.qqe(m5_close, 12, 10, 6.0);
										QqeResult m15_qqe = indicator.qqe(m15_close, 12, 10, 6.0);
										Map<String, Object> map = new HashMap<>();
										//Math.floor(value * 10000) / 10000.0;
										map.put("m5_ema25", Math.floor(m5_ema25 * 100000) / 100000.0);
										map.put("m5_ema99", Math.floor(m5_ema99 * 100000) / 100000.0);
										map.put("m15_ema25", Math.floor(m15_ema25 * 100000) / 100000.0);
										map.put("m15_ema99", Math.floor(m15_ema99 * 100000) / 100000.0);
										map.put("m5_qqe", (m5_qqe.getSmoothedRsi() - 50));
										map.put("m15_qqe", (m15_qqe.getSmoothedRsi() - 50));
										map.put("price", price);
										return Mono.just(map);
									})

							)
				);
		});
		
	}
}
