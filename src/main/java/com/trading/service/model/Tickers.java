package com.trading.service.model;

import java.util.List;

public class Tickers {
	  private List<String> symbol;						//거래 심볼
	  private List<String> priceChange;				//가격 변동량
	  private List<String> priceChangePercent;	//가격 변동 퍼센트
	  private List<String> weightedAvgPrice;		//가중 평균 가격
	  private List<String> lastPrice;						//마지막 거래 가격
	  private List<String> lastQty;						//마지막 거래 수량
	  private List<String> openPrice;					//24시간 전 시작 가격
	  private List<String> highPrice;					//24시간 내 최고 가격
	  private List<String> lowPrice;						//24시간 내 최저 가격
	  private List<String> volume;						//거래된 자산의 총 수량
	  private List<String> quoteVolume;				//거래된 견적 자산의 총 수량
	  private List<String> openTime;					//24시간 창의 시작 시간(타임스탬프)
	  private List<String> closeTime;					//24시간 창의 종료 시간(타임스탬프)
	
	public Tickers setTickers(List<Ticker> list) {
		this.symbol = list.stream().map(Ticker::getSymbol).toList();
		this.priceChange = list.stream().map(Ticker::getPriceChange).toList();
		this.priceChangePercent = list.stream().map(Ticker::getPriceChangePercent).toList();
		this.weightedAvgPrice = list.stream().map(Ticker::getWeightedAvgPrice).toList();
		this.lastPrice = list.stream().map(Ticker::getLastPrice).toList();
		this.lastQty = list.stream().map(Ticker::getLastQty).toList();
		this.openPrice = list.stream().map(Ticker::getOpenPrice).toList();
		this.highPrice = list.stream().map(Ticker::getHighPrice).toList();
		this.lowPrice = list.stream().map(Ticker::getLowPrice).toList();
		this.volume = list.stream().map(Ticker::getVolume).toList();
		this.quoteVolume = list.stream().map(Ticker::getQuoteVolume).toList();
		this.openTime = list.stream().map(Ticker::getOpenTime).toList();
		this.closeTime = list.stream().map(Ticker::getCloseTime).toList();
		return this;
	}

	public List<String> getSymbol() {
		return symbol;
	}

	public void setSymbol(List<String> symbol) {
		this.symbol = symbol;
	}

	public List<String> getPriceChange() {
		return priceChange;
	}

	public void setPriceChange(List<String> priceChange) {
		this.priceChange = priceChange;
	}

	public List<String> getPriceChangePercent() {
		return priceChangePercent;
	}

	public void setPriceChangePercent(List<String> priceChangePercent) {
		this.priceChangePercent = priceChangePercent;
	}

	public List<String> getWeightedAvgPrice() {
		return weightedAvgPrice;
	}

	public void setWeightedAvgPrice(List<String> weightedAvgPrice) {
		this.weightedAvgPrice = weightedAvgPrice;
	}

	public List<String> getLastPrice() {
		return lastPrice;
	}

	public void setLastPrice(List<String> lastPrice) {
		this.lastPrice = lastPrice;
	}

	public List<String> getLastQty() {
		return lastQty;
	}

	public void setLastQty(List<String> lastQty) {
		this.lastQty = lastQty;
	}

	public List<String> getOpenPrice() {
		return openPrice;
	}

	public void setOpenPrice(List<String> openPrice) {
		this.openPrice = openPrice;
	}

	public List<String> getHighPrice() {
		return highPrice;
	}

	public void setHighPrice(List<String> highPrice) {
		this.highPrice = highPrice;
	}

	public List<String> getLowPrice() {
		return lowPrice;
	}

	public void setLowPrice(List<String> lowPrice) {
		this.lowPrice = lowPrice;
	}

	public List<String> getVolume() {
		return volume;
	}

	public void setVolume(List<String> volume) {
		this.volume = volume;
	}

	public List<String> getQuoteVolume() {
		return quoteVolume;
	}

	public void setQuoteVolume(List<String> quoteVolume) {
		this.quoteVolume = quoteVolume;
	}

	public List<String> getOpenTime() {
		return openTime;
	}

	public void setOpenTime(List<String> openTime) {
		this.openTime = openTime;
	}

	public List<String> getCloseTime() {
		return closeTime;
	}

	public void setCloseTime(List<String> closeTime) {
		this.closeTime = closeTime;
	}

	@Override
	public String toString() {
		return "Tickers [symbol=" + symbol + ", priceChange=" + priceChange + ", priceChangePercent="
				+ priceChangePercent + ", weightedAvgPrice=" + weightedAvgPrice + ", lastPrice=" + lastPrice
				+ ", lastQty=" + lastQty + ", openPrice=" + openPrice + ", highPrice=" + highPrice + ", lowPrice="
				+ lowPrice + ", volume=" + volume + ", quoteVolume=" + quoteVolume + ", openTime=" + openTime
				+ ", closeTime=" + closeTime + "]";
	}
	
	
}
