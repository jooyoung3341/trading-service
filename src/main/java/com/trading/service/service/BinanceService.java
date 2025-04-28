package com.trading.service.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.trading.service.common.BinanceUtil;
import com.trading.service.model.EnumType;
import com.trading.service.model.PositionInfo;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Mono;

@Service
public class BinanceService {

	@Value("${binance.api-key}")
	private String apiKey;
	@Value("${binance.secret-key}")
	private String secretKey;
	
	private WebClient webClient;
	
	@Autowired
	BinanceUtil util;
	
    @PostConstruct
    public void init() {
        webClient = WebClient.builder()
                .baseUrl("https://fapi.binance.com")
                .defaultHeader("X-MBX-APIKEY", apiKey)
                .build();
    }

    //요청에 필요한 서명 만들기
    private String generateSignature(String data) {
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKeySpec);
            byte[] hash = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error while generating signature", e);
        }
    }
    
	/*
	 * ==========================================
	*/
    //내 선물 계좌 USDT 가져오기 => 잔고USDT 조회할려면 이거 부르면 됨
    public Mono<String> getUsdt() {
        long timestamp = Instant.now().toEpochMilli();
        String queryString = "timestamp=" + timestamp;
        String signature = generateSignature(queryString);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/fapi/v2/account")
                        .queryParam("timestamp", timestamp)
                        .queryParam("signature", signature)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .map(this::extractUSDTBalance);
    }

    
    //선물 포지션 여부 -> 이거 부르면 됨
    public Mono<Boolean> isPosition() {
        return getFuturesPositionInfo()
                .map(response -> {
                    var jsonArray = com.google.gson.JsonParser.parseString(response).getAsJsonArray();
                    for (var element : jsonArray) {
                        var obj = element.getAsJsonObject();
                        String positionAmt = obj.get("positionAmt").getAsString();
                        if (positionAmt != null && Double.parseDouble(positionAmt) != 0.0) {
                            return true; // 포지션 있음
                        }
                    }
                    return false; // 포지션 없음
                });
    }
    
    //선물 포지션 여부 -> 롱 숏 여부
    public Mono<String> isPositionType(String symbol) {
        return getFuturesPositionInfo()
                .map(response -> {
                    var jsonArray = com.google.gson.JsonParser.parseString(response).getAsJsonArray();
                    for (var element : jsonArray) {
                        var obj = element.getAsJsonObject();
                        if (symbol.equals(obj.get("symbol").getAsString())) {
                            double positionAmt = Double.parseDouble(obj.get("positionAmt").getAsString());
                            if (positionAmt > 0) {
                                return EnumType.Long.value(); // 롱 포지션
                            } else if (positionAmt < 0) {
                                return EnumType.Short.value(); // 숏 포지션
                            } else {
                                return EnumType.None.value(); // 포지션 없음
                            }
                        }
                    }
                    return EnumType.None.value(); // 해당 심볼이 없을 경우도 NONE
                });
    }
    
    //포지션 진입									심볼,     BUY = 롱 / SELL = 숏		사이즈
    //포지션이 있을 경우 반대로 하면 포지션 종료
    public Mono<String> openPosition(String symbol, String side, double quantity) {
        long timestamp = Instant.now().toEpochMilli();
        String queryString = "symbol=" + symbol +
                             "&side=" + side +
                             "&type=MARKET" +
                             "&quantity=" + quantity +
                             "&timestamp=" + timestamp;
        String signature = generateSignature(queryString);

        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/fapi/v1/order")
                        .queryParam("symbol", symbol)
                        .queryParam("side", side)
                        .queryParam("type", "MARKET")
                        .queryParam("quantity", quantity)
                        .queryParam("timestamp", timestamp)
                        .queryParam("signature", signature)
                        .build())
                .retrieve()
                .bodyToMono(String.class);
    }
    
    //레버리지 설정
    public Mono<String> setLeverage(String symbol, int leverage) {
        long timestamp = Instant.now().toEpochMilli();
        String queryString = "symbol=" + symbol +
                             "&leverage=" + leverage +
                             "&timestamp=" + timestamp;
        String signature = generateSignature(queryString);

        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/fapi/v1/leverage")
                        .queryParam("symbol", symbol)
                        .queryParam("leverage", leverage)
                        .queryParam("timestamp", timestamp)
                        .queryParam("signature", signature)
                        .build())
                .retrieve()
                .bodyToMono(String.class);
    }
    
    //포지션 잡을 수량 구하기
    public Mono<Double> getQuantity(String symbol, double walletUsdt, int leverage, double price) {
        return getQuantityBySymbol(symbol)
        .flatMap(precision -> {
            

            double totalUsdt = walletUsdt * leverage;
            double quantity = totalUsdt / price;

            // 소수점 자리수 맞춰주기
            double scale = Math.pow(10, precision);
            quantity = Math.floor(quantity * scale) / scale;

            return Mono.just(quantity);
        });
    }
 
     //손절라인 잡기
    public Mono<String> placeStopLoss(String symbol, String side, double stopPrice) {
        long timestamp = Instant.now().toEpochMilli();
        String queryString = "symbol=" + symbol +
                             "&side=" + side +
                             "&type=STOP_MARKET" +
                             "&stopPrice=" + stopPrice +
                             "&closePosition=true" +
                             "&timestamp=" + timestamp;
        String signature = generateSignature(queryString);

        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/fapi/v1/order")
                        .queryParam("symbol", symbol)
                        .queryParam("side", side)
                        .queryParam("type", "STOP_MARKET")
                        .queryParam("stopPrice", stopPrice)
                        .queryParam("closePosition", true)
                        .queryParam("timestamp", timestamp)
                        .queryParam("signature", signature)
                        .build())
                .retrieve()
                .bodyToMono(String.class);
    }
    
    //익절라인 잡기
    public Mono<String> placeTakeProfit(String symbol, String side, double stopPrice) {
        long timestamp = Instant.now().toEpochMilli();
        String queryString = "symbol=" + symbol +
                             "&side=" + side +
                             "&type=TAKE_PROFIT_MARKET" +
                             "&stopPrice=" + stopPrice +
                             "&closePosition=true" +
                             "&timestamp=" + timestamp;
        String signature = generateSignature(queryString);

        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/fapi/v1/order")
                        .queryParam("symbol", symbol)
                        .queryParam("side", side)
                        .queryParam("type", "TAKE_PROFIT_MARKET")
                        .queryParam("stopPrice", stopPrice)
                        .queryParam("closePosition", true)
                        .queryParam("timestamp", timestamp)
                        .queryParam("signature", signature)
                        .build())
                .retrieve()
                .bodyToMono(String.class);
    }
    
    //포지션 정보 가져오기
    public Mono<PositionInfo> getPositionInfoModel(String symbol) {
        return getPositionInfo(symbol)
                .map(json -> {
                    PositionInfo info = new PositionInfo();
                    info.setSymbol(symbol);
                    info.setEntryPrice(json.get("entryPrice").getAsDouble());
                    info.setMarkPrice(json.get("markPrice").getAsDouble());
                    info.setUnRealizedProfit(json.get("unRealizedProfit").getAsDouble());
                    info.setLeverage(json.get("leverage").getAsInt());
                    info.setPositionAmt(json.get("positionAmt").getAsDouble());
                    return info;
                });
    }
    
	/*
	 * ================================================
	*/    
    //선물 포지션 여부
    public Mono<String> getFuturesPositionInfo() {
        long timestamp = Instant.now().toEpochMilli();
        String queryString = "timestamp=" + timestamp;
        String signature = generateSignature(queryString);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/fapi/v2/positionRisk")
                        .queryParam("timestamp", timestamp)
                        .queryParam("signature", signature)
                        .build())
                .retrieve()
                .bodyToMono(String.class);
    }
    //결과 JSON에서 USDT만 뽑아내기
    private String extractUSDTBalance(String jsonResponse) {
        // 간단한 파싱
        try {
            com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(jsonResponse).getAsJsonObject();
            for (var asset : jsonObject.getAsJsonArray("assets")) {
                var obj = asset.getAsJsonObject();
                if ("USDT".equals(obj.get("asset").getAsString())) {
                    return obj.get("walletBalance").getAsString(); // 내 USDT 지갑 잔고
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse USDT balance", e);
        }
        return "0";
    }
    
    public Mono<Integer> getQuantityBySymbol(String symbol) {
        return webClient.get()
                .uri("/fapi/v1/exchangeInfo")
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    JsonObject root = JsonParser.parseString(response).getAsJsonObject();
                    JsonArray symbols = root.getAsJsonArray("symbols");
                    for (var symbolElement : symbols) {
                        JsonObject symbolObj = symbolElement.getAsJsonObject();
                        String symbolName = symbolObj.get("symbol").getAsString();
                        if (symbol.equals(symbolName)) {
                            return symbolObj.get("quantityPrecision").getAsInt();
                        }
                    }
                    throw new RuntimeException("Symbol not found: " + symbol);
                });
    }
    

    
    public Mono<JsonObject> getPositionInfo(String symbol) {
        long timestamp = Instant.now().toEpochMilli();
        String queryString = "timestamp=" + timestamp;
        String signature = generateSignature(queryString);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/fapi/v2/positionRisk")
                        .queryParam("timestamp", timestamp)
                        .queryParam("signature", signature)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    JsonArray jsonArray = JsonParser.parseString(response).getAsJsonArray();
                    for (var element : jsonArray) {
                        JsonObject obj = element.getAsJsonObject();
                        if (symbol.equals(obj.get("symbol").getAsString())) {
                            return obj;
                        }
                    }
                    throw new RuntimeException("포지션 정보 없음: " + symbol);
                });
    }
    
}
