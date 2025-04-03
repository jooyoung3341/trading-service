package com.trading.service.ts.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.trading.service.ts.model.Candle;
import com.trading.service.ts.service.BinanceRestService;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class BinanceRestController {

	@Autowired
    private BinanceRestService restService;

    @GetMapping("/{interval}")
    public Mono<List<Candle>> getCandles(@PathVariable String interval) {
    	System.out.println("??");
        return restService.getCandles("BTCUSDT", interval, 100);
    }
}
