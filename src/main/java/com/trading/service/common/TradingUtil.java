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
}
