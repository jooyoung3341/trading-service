package com.trading.service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

@Service
public class TeleService {
	
	private final WebClient webClient;
    
	private final String botToken = "7918351725:AAEdRQDMUyr88ARq3Rlp6QNJC4pGsDFPwP8";
    private final String baseUrl = "https://api.telegram.org";
    private final String chatId = "2121764851";
    
    public TeleService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }
    
    public Mono<String> sendMessage(String text) {
        return webClient.post()
            .uri("/bot{token}/sendMessage", botToken)
            .bodyValue(new SendMessageRequest(chatId, text))
            .retrieve()
            .bodyToMono(String.class); // 응답을 String으로 받을 수 있음
    }

    // 내부 요청 DTO
    private static class SendMessageRequest {
        private final String chat_id;
        private final String text;

        public SendMessageRequest(String chatId, String text) {
            this.chat_id = chatId;
            this.text = text;
        }

        public String getChat_id() {
            return chat_id;
        }

        public String getText() {
            return text;
        }
    }
}
