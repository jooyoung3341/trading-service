package com.trading.service.model;

import java.util.List;

public class TradingData {

	private String symbol;
	private String m15_trand;
	private List<TradingData> td;
	
	
	public List<TradingData> getTd() {
		
		return td;
	}
	public void setTd(List<TradingData> td) {
		TradingData td1 = new TradingData();
		TradingData td2 = new TradingData();
		TradingData td3 = new TradingData();
		td1.setSymbol("BTCUSDT");
		td2.setSymbol("ETHUSDT");
		td3.setSymbol("SOLUSDT");
		td.add(td1);
		td.add(td2);
		td.add(td3);
		this.td = td;
	
	}
	public String getSymbol() {
		return symbol;
	}
	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}
	public String getM15_trand() {
		return m15_trand;
	}
	public void setM15_trand(String m15_trand) {
		this.m15_trand = m15_trand;
	}
	
	
}
