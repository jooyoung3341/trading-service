package com.trading.service.model;

import java.util.List;

public class Ticker {
	  private String symbol;						//거래 심볼
	  private String priceChange;				//가격 변동량
	  private String priceChangePercent;	//가격 변동 퍼센트
	  private String weightedAvgPrice;		//가중 평균 가격
	  private String lastPrice;						//마지막 거래 가격
	  private String lastQty;						//마지막 거래 수량
	  private String openPrice;					//24시간 전 시작 가격
	  private String highPrice;					//24시간 내 최고 가격
	  private String lowPrice;						//24시간 내 최저 가격
	  private String volume;						//거래된 자산의 총 수량
	  private String quoteVolume;				//거래된 견적 자산의 총 수량
	  private String openTime;					//24시간 창의 시작 시간(타임스탬프)
	  private String closeTime;					//24시간 창의 종료 시간(타임스탬프)
	  private String price;
	  private String m1_trand;
	  private String m5_trand;
	  private String m15_trand;
	  
	  
	public Ticker(String symbol, String priceChange, String priceChangePercent, String weightedAvgPrice,
			String lastPrice, String lastQty, String openPrice, String highPrice, String lowPrice, String volume,
			String quoteVolume, String openTime, String closeTime) {
		this.symbol = symbol; this.priceChange = priceChange; this.priceChangePercent = priceChangePercent; this.weightedAvgPrice = weightedAvgPrice;
		this.lastPrice = lastPrice; this.lastQty = lastQty; this.openPrice = openPrice; this.highPrice = highPrice; this.lowPrice = lowPrice; this.volume = volume;
		this.quoteVolume = quoteVolume; this.openTime = openTime; this.closeTime = closeTime;
	}
	
	public Ticker() {
		
	}
	public String getPrice() {
		return price;
	}


	public void setPrice(String price) {
		this.price = price;
	}


	public String getSymbol() {
		return symbol;
	}
	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}
	public String getPriceChange() {
		return priceChange;
	}
	public void setPriceChange(String priceChange) {
		this.priceChange = priceChange;
	}
	public String getPriceChangePercent() {
		return priceChangePercent;
	}
	public void setPriceChangePercent(String priceChangePercent) {
		this.priceChangePercent = priceChangePercent;
	}
	public String getWeightedAvgPrice() {
		return weightedAvgPrice;
	}
	public void setWeightedAvgPrice(String weightedAvgPrice) {
		this.weightedAvgPrice = weightedAvgPrice;
	}
	public String getLastPrice() {
		return lastPrice;
	}
	public void setLastPrice(String lastPrice) {
		this.lastPrice = lastPrice;
	}
	public String getLastQty() {
		return lastQty;
	}
	public void setLastQty(String lastQty) {
		this.lastQty = lastQty;
	}
	public String getOpenPrice() {
		return openPrice;
	}
	public void setOpenPrice(String openPrice) {
		this.openPrice = openPrice;
	}
	public String getHighPrice() {
		return highPrice;
	}
	public void setHighPrice(String highPrice) {
		this.highPrice = highPrice;
	}
	public String getLowPrice() {
		return lowPrice;
	}
	public void setLowPrice(String lowPrice) {
		this.lowPrice = lowPrice;
	}
	public String getVolume() {
		return volume;
	}
	public void setVolume(String volume) {
		this.volume = volume;
	}
	public String getQuoteVolume() {
		return quoteVolume;
	}
	public void setQuoteVolume(String quoteVolume) {
		String volStr = "";
		if(quoteVolume.contains(".")) {
			String[] volAry = quoteVolume.split("\\.");	
			volStr = volAry[0];
		}else {
			volStr = quoteVolume;
		}
		this.quoteVolume = volStr;
	}
	public String getOpenTime() {
		return openTime;
	}
	public void setOpenTime(String openTime) {
		this.openTime = openTime;
	}
	public String getCloseTime() {
		return closeTime;
	}
	public void setCloseTime(String closeTime) {
		this.closeTime = closeTime;
	}
	


	public String getM1_trand() {
		return m1_trand;
	}


	public void setM1_trand(String m1_trand) {
		this.m1_trand = m1_trand;
	}


	public String getM5_trand() {
		return m5_trand;
	}


	public void setM5_trand(String m5_trand) {
		this.m5_trand = m5_trand;
	}


	public String getM15_trand() {
		return m15_trand;
	}


	public void setM15_trand(String m15_trand) {
		this.m15_trand = m15_trand;
	}


	@Override
	public String toString() {
		return "Ticker [symbol=" + symbol + ", priceChange=" + priceChange + ", priceChangePercent="
				+ priceChangePercent + ", weightedAvgPrice=" + weightedAvgPrice + ", lastPrice=" + lastPrice
				+ ", lastQty=" + lastQty + ", openPrice=" + openPrice + ", highPrice=" + highPrice + ", lowPrice="
				+ lowPrice + ", volume=" + volume + ", quoteVolume=" + quoteVolume + ", openTime=" + openTime
				+ ", closeTime=" + closeTime + ", getSymbol()=" + getSymbol() + ", getPriceChange()=" + getPriceChange()
				+ ", getPriceChangePercent()=" + getPriceChangePercent() + ", getWeightedAvgPrice()="
				+ getWeightedAvgPrice() + ", getLastPrice()=" + getLastPrice() + ", getLastQty()=" + getLastQty()
				+ ", getOpenPrice()=" + getOpenPrice() + ", getHighPrice()=" + getHighPrice() + ", getLowPrice()="
				+ getLowPrice() + ", getVolume()=" + getVolume() + ", getQuoteVolume()=" + getQuoteVolume()
				+ ", getOpenTime()=" + getOpenTime() + ", getCloseTime()=" + getCloseTime() + ", getClass()="
				+ getClass() + ", hashCode()=" + hashCode() + ", toString()=" + super.toString() + "]";
	}
	  
	
}
