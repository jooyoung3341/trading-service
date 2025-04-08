package com.trading.service.common;

import java.util.List;

import org.springframework.stereotype.Component;

import com.trading.service.model.Candle;
import com.trading.service.model.Ticker;

@Component
public class MapToModel {

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
		
	//getCandles에서 가져온 데이터를 객체화 시킴
	private List<Ticker> mapToTicker24h(List<List<Object>> raw) {
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
