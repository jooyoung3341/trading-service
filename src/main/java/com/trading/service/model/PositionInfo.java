package com.trading.service.model;

public class PositionInfo {
	   // 거래쌍 이름 (예: BTCUSDT, ETHUSDT 등)
    private String symbol;
    // 내가 진입한 평균 가격
    private double entryPrice;
    // 현재 시장 가격 (Mark Price)
    private double markPrice;
    // 현재 미실현 손익 (수익 or 손실, 아직 확정되지 않은 상태)
    private double unRealizedProfit;
    // 현재 포지션 레버리지 배율 (예: 5배, 10배)
    private int leverage;
    // 현재 보유 중인 포지션 수량 (롱이면 양수, 숏이면 음수)
    private double positionAmt;
    
	public String getSymbol() {
		return symbol;
	}
	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}
	public double getEntryPrice() {
		return entryPrice;
	}
	public void setEntryPrice(double entryPrice) {
		this.entryPrice = entryPrice;
	}
	public double getMarkPrice() {
		return markPrice;
	}
	public void setMarkPrice(double markPrice) {
		this.markPrice = markPrice;
	}
	public double getUnRealizedProfit() {
		return unRealizedProfit;
	}
	public void setUnRealizedProfit(double unRealizedProfit) {
		this.unRealizedProfit = unRealizedProfit;
	}
	public int getLeverage() {
		return leverage;
	}
	public void setLeverage(int leverage) {
		this.leverage = leverage;
	}
	public double getPositionAmt() {
		return positionAmt;
	}
	public void setPositionAmt(double positionAmt) {
		this.positionAmt = positionAmt;
	}
	@Override
	public String toString() {
		return "PositionInfo [symbol=" + symbol + ", entryPrice=" + entryPrice + ", markPrice=" + markPrice
				+ ", unRealizedProfit=" + unRealizedProfit + ", leverage=" + leverage + ", positionAmt=" + positionAmt
				+ "]";
	}
    
    
}
