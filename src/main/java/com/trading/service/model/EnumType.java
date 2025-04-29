package com.trading.service.model;

public enum EnumType {

	Success("Success")
	,Fail("Fail")
	,None("None")
	,Long("Long")
	,Short("Short")
	,autoSymbol("autoSymbol")
	,TradingSymbol("TradingSymbol")
	,isAuto("isAuto")
	,DetailSymbol("DetailSymbol")
	,m1("1m")
	,m5("5m")
	,m15("15m")
	,h1("1h")
	,x("X")
	,o("O")
	,BUY("BUY")
	,SELL("SELL")
	,candle("110")
	,m5_tele("m5tele_")
	,m15_tele("m15tele_")
	,symbol("symbol")

	
	;
	final String value;
	
	EnumType(String value) {
       this.value = value;
   }

	
   public String value() {
       return value;
   }
}
