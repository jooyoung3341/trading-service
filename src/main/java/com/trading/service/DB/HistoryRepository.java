package com.trading.service.DB;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.trading.service.model.TradingHistory;

public interface HistoryRepository extends ReactiveCrudRepository<TradingHistory, Long>{

}
