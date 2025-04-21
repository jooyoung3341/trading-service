package com.trading.service.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

@Service
public class RedisService {

	
	@Autowired
    private ReactiveRedisTemplate<String, String> redisTemplate;

    /*public MyRedisService(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }*/

    // 저장
    public Mono<Boolean> saveValue(String key, String value) {
        return redisTemplate.opsForValue().set(key, value);
    }

    // 조회
    public Mono<String> getValue(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    // 삭제
    public Mono<Long> delete(String key) {
        return redisTemplate.delete(key);
    }
    
    //전체 조회하기
    public Mono<List<String>> getTradingSymbolList(String key) {
        return redisTemplate.opsForList()
                .range(key, 0, -1) // 0부터 끝(-1)까지 전체 조회
                .collectList();                // Flux<String> → Mono<List<String>>
    }
    
    // 리스트에 추가
    public Mono<Long> addTradingSymbol(String key, String value) {
        return redisTemplate.opsForList()
                .rightPush(key, value);
    }

    // 리스트에서 삭제
    public Mono<Long> removeTradingSymbol(String key, String value) {
        return redisTemplate.opsForList()
                .remove(key, 0, value);
    }
}
