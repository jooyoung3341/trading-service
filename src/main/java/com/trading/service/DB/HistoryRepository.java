package com.trading.service.DB;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;

public interface HistoryRepository extends ReactiveCrudRepository<History, Long>{

	@Query("SELECT * FROM history WHERE openTime BETWEEN :start AND :end")
	Flux<History> getOpenTime(String start, String end);
}
