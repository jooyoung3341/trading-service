package com.trading.service.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.trading.service.model.Ticker24h;
import com.trading.service.service.BinanceRestService;

import reactor.core.publisher.Mono;

@Controller
public class WebController {

	@Autowired
	BinanceRestService restService;
	
	@ResponseBody
	@RequestMapping(value="/", method=RequestMethod.GET)
	public Mono<String> home(Model model) {
		return Mono.just("web/home");
	}
	
	/*@ResponseBody
	@RequestMapping(value="/getTicker", method=RequestMethod.GET)
	public Mono<List<Ticker24h>> getTicker() {
		return Mono.defer(() -> restService.getTicker24H()
					
		)
		}*/
				
				
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
