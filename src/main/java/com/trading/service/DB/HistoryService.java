package com.trading.service.DB;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class HistoryService {
	@Autowired
	private HistoryRepository repo;

	   public Flux<History> getAll() {
	        return repo.findAll();
	    }

	    public Mono<History> save(History history) {
	        return repo.save(history);
	    }
	    
	    public Flux<History> getByCloseTimeRange(String start, String end) {
	        return repo.findByCloseTimeBetween(start, end);
	    }
}
