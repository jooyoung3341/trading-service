package com.trading.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

import com.trading.service.service.BacktestService;
import com.trading.service.service.BinanceRestService;

@SpringBootApplication
public class TradingServiceApplication {

	public static void main(String[] args) {
        //ConfigurableApplicationContext context = SpringApplication.run(TradingServiceApplication.class, args);

		SpringApplication.run(TradingServiceApplication.class, args);
        /*@Bean
		WebClient webClient = WebClient.builder()
	            .baseUrl("https://fapi.binance.com") // 바이낸스 선물 API base URL
	            .build();*/

		BinanceRestService candleService = new BinanceRestService();
	    BacktestService backtestService = new BacktestService(candleService);

	    backtestService.runBacktest("BTCUSDT"); // BTCUSDT 5m봉 1500개로 테스트 실행

	}

}
