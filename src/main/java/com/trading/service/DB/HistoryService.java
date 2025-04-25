package com.trading.service.DB;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class HistoryService {
	@Autowired
	private HistoryRepository repo;
	@Autowired
    private R2dbcEntityTemplate r2dbcEntityTemplate;

	/*
		ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'Welcome1!';
		FLUSH PRIVILEGES;
		SELECT user, host, plugin FROM mysql.user WHERE user = 'root';
		
		SHOW VARIABLES LIKE '%time_zone%';
		SET GLOBAL time_zone = '+09:00';
	*/
	   public Flux<History> getAll() {
	        return repo.findAll();
	    }

	    public Mono<History> save(History history) {
	        return repo.save(history);
	    }
	    
	    public Flux<History> getByCloseTimeRange(String start, String end) {
	        return repo.findByCloseTimeBetween(start, end);
	    }
	    
	    public Mono<History> insertHistory(History history) {
	        return r2dbcEntityTemplate.insert(History.class).using(history);
	    }
}
