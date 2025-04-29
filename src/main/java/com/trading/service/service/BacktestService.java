package com.trading.service.service;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.trading.service.BacktestRunner;
import com.trading.service.common.Indicator;
import com.trading.service.model.Candle;

@Service
public class BacktestService {

	
	private final BinanceRestService candleService;
	private final BacktestRunner backtestRunner;

	    public BacktestService(BinanceRestService candleService) {
	        this.candleService = candleService;
	        this.backtestRunner = new BacktestRunner();
	    }

	    public void runBacktest(String symbol) {
	    	List<Candle> candles = candleService.getCandles("BTCUSDT", "5m", 1).block();
	    	System.out.println("받은 캔들 시간: " + Instant.ofEpochMilli(candles.get(0).getOpenTime()));
	        List<Candle> m15_candles = candleService.getCandles(symbol, "1h", 1500).block();

	        List<Candle> m5_candles = candleService.getCandles(symbol, "30m", 1500).block();


	        System.out.println("✅ 5m캔들 수: " + m5_candles.size());
	        System.out.println("✅ 15m캔들 수: " + m15_candles.size());

	        backtestRunner.run(m5_candles, m15_candles);
	    }

	
}
