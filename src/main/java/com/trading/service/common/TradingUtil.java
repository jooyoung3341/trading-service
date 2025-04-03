package com.trading.service.common;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.springframework.stereotype.Component;

@Component
public class TradingUtil {

	public long toKst(long time) {
		  return Instant.ofEpochMilli(time)
		            .atZone(ZoneOffset.UTC) // 원래는 UTC 기준
		            .withZoneSameInstant(ZoneId.of("Asia/Seoul")) // KST로 변환
		            .toInstant()
		            .toEpochMilli(); // 다시 long으로 리턴
	}
}
