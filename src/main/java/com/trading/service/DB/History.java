package com.trading.service.DB;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("history")
public class History {
	@Id
	private long timeSeq;				// = pk
	private String symbol; 			//= 심볼
	private LocalDateTime openTime;			// = 오픈시간
	private String openPrice;			// = 오픈가격
	private LocalDateTime closeTime; 		//= 닫은시간
	private String closePrice;		// = 닫은시간
	private String qtyOpenPrice; 	//= 들어간 자산 가격
	private String qtyClosePrice; 	//= 종료 후 자산 가격
	private String percent; 			//= 이 포지션으로 얻은 percent
	private String ising; 				//= x 면 포지션 유지중 , o면 포지션 종료
	private String trand;  				//포지션타입 (long, short)
	/*
	 * CREATE TABLE `trading`.`history` (
	  `timeSeq` INT NOT NULL,
	  `symbol` VARCHAR(45) NULL,
	  `openTime` VARCHAR(45) NULL,
	  `openPrice` VARCHAR(45) NULL,
	  `closeTime` VARCHAR(45) NULL,
	  `closePrice` VARCHAR(45) NULL,
	  `qtyOpenPrice` VARCHAR(45) NULL,
	  `qtyClosePrice` VARCHAR(45) NULL,
	  `percent` VARCHAR(45) NULL,
	  `ising` VARCHAR(45) NULL,
	  PRIMARY KEY (`timeSeq`));
	*/


	
	public String getTrand() {
		return trand;
	}


	public void setTrand(String trand) {
		this.trand = trand;
	}


	public long getTimeSeq() {
		return timeSeq;
	}

	public void setTimeSeq(long timeSeq) {
		this.timeSeq = timeSeq;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public LocalDateTime getOpenTime() {
		return openTime;
	}

	public void setOpenTime(LocalDateTime openTime) {
		this.openTime = openTime;
	}

	public String getOpenPrice() {
		return openPrice;
	}

	public void setOpenPrice(String openPrice) {
		this.openPrice = openPrice;
	}

	public LocalDateTime getCloseTime() {
		return closeTime;
	}

	public void setCloseTime(LocalDateTime closeTime) {
		this.closeTime = closeTime;
	}

	public String getClosePrice() {
		return closePrice;
	}

	public void setClosePrice(String closePrice) {
		this.closePrice = closePrice;
	}

	public String getQtyOpenPrice() {
		return qtyOpenPrice;
	}

	public void setQtyOpenPrice(String qtyOpenPrice) {
		this.qtyOpenPrice = qtyOpenPrice;
	}

	public String getQtyClosePrice() {
		return qtyClosePrice;
	}

	public void setQtyClosePrice(String qtyClosePrice) {
		this.qtyClosePrice = qtyClosePrice;
	}

	public String getPercent() {
		return percent;
	}

	public void setPercent(String percent) {
		this.percent = percent;
	}

	public String getIsing() {
		return ising;
	}

	public void setIsing(String ising) {
		this.ising = ising;
	}
	

}
