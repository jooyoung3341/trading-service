package com.trading.service.common;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.springframework.stereotype.Component;

@Component
public class TradingUtil {

	public LocalDateTime toKst(long time) {
		return Instant.ofEpochMilli(time)
		        .atZone(ZoneId.of("Asia/Seoul"))
		        .toLocalDateTime();
	}
	
    public double calculatePercentageChange(double basePrice, double percent) {
        return basePrice + (basePrice * percent / 100.0);
    }
    
    public double minusPercent(double price, double percenter) {
    	return calculatePercentageChange(price, -percenter);
    }
    
    public double plusPercent(double price, double percenter) {
    	return calculatePercentageChange(price, percenter);
    }
}
