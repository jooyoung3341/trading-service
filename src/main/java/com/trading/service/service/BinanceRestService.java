package com.trading.service.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.json.JSONObject;

import com.trading.service.common.MapToModel;
import com.trading.service.model.Candle;
import com.trading.service.model.Ticker;
import com.trading.service.model.Tickers;

import reactor.core.publisher.Mono;

@Service
public class BinanceRestService {
    
	private static final Logger log = LoggerFactory.getLogger(BinanceRestService.class);
	
	@Autowired
	private MapToModel toModel;
	
	private final WebClient webClient = WebClient.builder()
		        .baseUrl("https://fapi.binance.com")
		        .build();

	//캔들데이터 가져오기
	public Mono<List<Candle>> getCandles(String symbol, String interval, int limit) {
		return webClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/fapi/v1/klines")
						.queryParam("symbol", symbol)
						.queryParam("interval", interval)
						.queryParam("limit", limit)
						.build())
		            	.retrieve()
		            	.bodyToMono(new ParameterizedTypeReference<List<List<Object>>>() {})
		            	.map(this::mapToCandles);
		            	//.doOnSuccess(result -> log.info("✅ [요청 성공] 받은 캔들 수: {}", result.size()))
		                //.doOnError(error -> log.error("❌ [요청 실패] Binance API 호출 실패: {}", error.getMessage()));
		}
	//getCandles에서 가져온 데이터를 객체화 시킴
	private List<Candle> mapToCandles(List<List<Object>> raw) {
		return raw.stream().map(data -> {
			long openTime = ((Number) data.get(0)).longValue(); // 캔들생성시간
			double open = Double.parseDouble(data.get(1).toString()); // 시가
			double high = Double.parseDouble(data.get(2).toString()); // 캔들 중 가장 높은 가격
			double low = Double.parseDouble(data.get(3).toString()); // 캔들 중 가장 낮은 가격
			double close = Double.parseDouble(data.get(4).toString()); // 종가
			double volume = Double.parseDouble(data.get(5).toString()); // 거래량
			return new Candle(openTime, open, high, low, close, volume);
		}).toList();
	}
	
	//현재 가격 가져오기
    public Mono<Double> getPrice(String symbol) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/fapi/v1/ticker/price")
                        .queryParam("symbol", symbol)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    JSONObject json = new JSONObject(response);
                    return json.getDouble("price");
                });
    }
    
    //심볼에 대한 현제 데이터 (symbol없으면 전체코인 가져옴)
    public Mono<List<Ticker>> getTicker(String symbol){
    	return webClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/fapi/v1/ticker/24hr")
						.queryParam("symbol", symbol)
						.build())
		            	.retrieve()
		            	.bodyToMono(Ticker.class)
		            	.map(List::of);
		            	//.doOnSuccess(result -> log.info("✅ [요청 성공] 받은 캔들 수: {}", result.size()))
		                //.doOnError(error -> log.error("❌ [요청 실패] Binance API 호출 실패: {}", error.getMessage()));
    }
    
    //심볼에 대한 현재 데이터 (symbol없으면 전체코인 가져옴)
    public Mono<List<Ticker>> getTickers(){
    	return webClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/fapi/v1/ticker/24hr")
						.build())
		            	.retrieve()
		            	.bodyToMono(new ParameterizedTypeReference<List<Ticker>>() {});
		            	//.map(this::mapToTicker)
		            	//.doOnSuccess(result -> log.info("✅ [요청 성공] 받은 캔들 수: {}", result.size()))
		                //.doOnError(error -> log.error("❌ [요청 실패] Binance API 호출 실패: {}", error.getMessage()));
    }
    
    //getTicker24H에서 가져온 데이터를 객체화 시킴
  	private List<Ticker> mapToTickers(List<List<Object>> raw) {
  		return raw.stream().map(data -> {
  			String symbol = data.get(0).toString(); 					// 거래 심볼
  			String priceChange = data.get(1).toString();				//가격 변동량
  			String priceChangePercent = data.get(2).toString();	//가격 변동 퍼센트
  			String weightedAvgPrice = data.get(3).toString();		//가중 평균 가격
  			String lastPrice = data.get(4).toString();					//마지막 거래 가격
  			String lastQty = data.get(5).toString();						//마지막 거래 수량
  			String openPrice = data.get(6).toString();					//24시간 전 시작 가격
  			String highPrice = data.get(7).toString();					//24시간 내 최고 가격
  			String lowPrice = data.get(8).toString();					//24시간 내 최저 가격
  			String volume = data.get(9).toString();					//거래된 자산의 총 수량
  			String quoteVolume = data.get(10).toString();			//거래된 견적 자산의 총 수량
  			String openTime = data.get(11).toString();				//24시간 창의 시작 시간(타임스탬프)
  			String closeTime = data.get(12).toString();				//24시간 창의 종료 시간(타임스탬프)
  			return new Ticker(symbol, priceChange, priceChangePercent, weightedAvgPrice, lastPrice, 
  					lastQty, openPrice, highPrice, lowPrice, volume, quoteVolume, openTime, closeTime);
  		}).toList();
  	}
    
  //getTicker24H에서 가져온 데이터를 객체화 시킴
  	private List<Ticker> mapToTicker(List<List<Object>> raw) {
  		return raw.stream().map(data -> {
  			String symbol = data.get(0).toString(); 					// 거래 심볼
  			String priceChange = data.get(1).toString();				//가격 변동량
  			String priceChangePercent = data.get(2).toString();	//가격 변동 퍼센트
  			String weightedAvgPrice = data.get(3).toString();		//가중 평균 가격
  			String lastPrice = data.get(4).toString();					//마지막 거래 가격
  			String lastQty = data.get(5).toString();						//마지막 거래 수량
  			String openPrice = data.get(6).toString();					//24시간 전 시작 가격
  			String highPrice = data.get(7).toString();					//24시간 내 최고 가격
  			String lowPrice = data.get(8).toString();					//24시간 내 최저 가격
  			String volume = data.get(9).toString();					//거래된 자산의 총 수량
  			String quoteVolume = data.get(10).toString();			//거래된 견적 자산의 총 수량
  			String openTime = data.get(11).toString();				//24시간 창의 시작 시간(타임스탬프)
  			String closeTime = data.get(12).toString();				//24시간 창의 종료 시간(타임스탬프)
  			return new Ticker(symbol, priceChange, priceChangePercent, weightedAvgPrice, lastPrice, 
  					lastQty, openPrice, highPrice, lowPrice, volume, quoteVolume, openTime, closeTime);
  		}).toList();
  	}
}
