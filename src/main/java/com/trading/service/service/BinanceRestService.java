package com.trading.service.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.trading.service.model.Candle;

import reactor.core.publisher.Mono;

@Service
public class BinanceRestService {
    
	private static final Logger log = LoggerFactory.getLogger(BinanceRestService.class);

	private final WebClient webClient = WebClient.builder()
		        .baseUrl("https://api.binance.com")
		        .build();

	  public Mono<List<Candle>> getCandles(String symbol, String interval, int limit) {
		  String url = UriComponentsBuilder.fromHttpUrl("https://api.binance.com/api/v3/klines")
		            .queryParam("symbol", symbol)
		            .queryParam("interval", interval)
		            .queryParam("limit", limit)
		            .build()
		            .toUriString();

		    log.info("📤 [요청 시작] Binance API 호출 → {}", url);
		  return webClient.get()
				  .uri(uriBuilder -> uriBuilder
						  .path("/api/v3/klines")
						  .queryParam("symbol", symbol)
						  .queryParam("interval", interval)
						  .queryParam("limit", limit)
						  .build())
		            	.retrieve()
		            	.bodyToMono(new ParameterizedTypeReference<List<List<Object>>>() {})
		            	.map(this::mapToCandles)
		            	.doOnSuccess(result -> log.info("✅ [요청 성공] 받은 캔들 수: {}", result.size()))
		                .doOnError(error -> log.error("❌ [요청 실패] Binance API 호출 실패: {}", error.getMessage()));
	  }

		    private List<Candle> mapToCandles(List<List<Object>> raw) {
		        return raw.stream().map(data -> {
		            long openTime = ((Number) data.get(0)).longValue();
		            double open = Double.parseDouble(data.get(1).toString());
		            double high = Double.parseDouble(data.get(2).toString());
		            double low = Double.parseDouble(data.get(3).toString());
		            double close = Double.parseDouble(data.get(4).toString());
		            double volume = Double.parseDouble(data.get(5).toString());
		            return new Candle(openTime, open, high, low, close, volume);
		        }).toList();
		    }
}
