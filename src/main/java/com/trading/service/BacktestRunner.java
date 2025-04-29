package com.trading.service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import com.trading.service.common.Indicator;
import com.trading.service.model.Candle;
import com.trading.service.model.EnumType;

public class BacktestRunner {

    private static final double ENTRY_FEE_RATE = 0.001; // 왕복 0.1%
    private static final double TAKE_PROFIT_RATE = 0.015; // +1%
    private static final double STOP_LOSS_RATE = 0.01;   // -1%
    private static final double LEVERAGE = 5.0;
    private static final double INITIAL_BALANCE = 10000.0;

    private double balance = INITIAL_BALANCE;
    private double entryPrice = 0.0;
    private String position = EnumType.None.value(); // "LONG", "SHORT", "NONE"
    private String savedTrend = EnumType.None.value(); // "LONG", "SHORT"
    private int totalTrades = 0;
    private int winTrades = 0;
    private double maxDrawdown = 0.0;
    private double peakBalance = INITIAL_BALANCE;

    public void run(List<Candle> m5Candles, List<Candle> m15Candles) {

        // 5분봉 마지막 시점 기준으로 15분봉 잘라내기
        long last5MinOpenTime = m5Candles.get(m5Candles.size() - 1).getOpenTime();
        List<Candle> filteredM15Candles = m15Candles.stream()
            .filter(candle -> candle.getOpenTime() <= last5MinOpenTime)
            .collect(Collectors.toList());

        for (int i = 60; i < m5Candles.size(); i++) {

            List<Double> m5Closes = m5Candles.subList(0, i + 1).stream()
                    .map(Candle::getClose)
                    .collect(Collectors.toList());

            int m15Index = (i / 3); // 5분봉 3개 == 15분봉 1개
            if (m15Index >= filteredM15Candles.size() || m15Index < 60) continue;

            List<Double> m15Closes = filteredM15Candles.subList(0, m15Index + 1).stream()
                    .map(Candle::getClose)
                    .collect(Collectors.toList());

            String m15Trend = determineTrend(m15Closes);
            String prevSTC5 = Indicator.getSTC(m5Closes.subList(0, m5Closes.size() - 1), 80, 27, 50).block();
            String currentSTC5 = Indicator.getSTC(m5Closes, 80, 27, 50).block();

            Candle currentCandle = m5Candles.get(i);
            String candleTime = Instant.ofEpochMilli(currentCandle.getOpenTime()).toString(); // UTC 시간 출력

            // 진입 조건
            if ("Long".equals(m15Trend)) {
                if ("Short".equals(prevSTC5) && "Long".equals(currentSTC5) && "None".equals(position)) {
                	System.out.println("➡️ 롱 진입 시도!");
                    openLong(currentCandle.getClose());
                }
            } else if ("Short".equals(m15Trend)) {
                if ("Long".equals(prevSTC5) && "Short".equals(currentSTC5) && "None".equals(position)) {
                	System.out.println("➡️ 숏 진입 시도!");
                    openShort(currentCandle.getClose());
                }
            }

            // 포지션 종료 조건
            if (!"None".equals(position)) {
                if (shouldClosePosition(currentCandle.getClose())) {
                    closePosition(currentCandle.getClose());
                }
            }
        }

        printResult();
    }

    private void openLong(double price) {
        entryPrice = price * (1 + ENTRY_FEE_RATE / 2);
        position = "Long";
        System.out.println("📈 LONG 진입 at " + entryPrice);
    }

    private void openShort(double price) {
        entryPrice = price * (1 - ENTRY_FEE_RATE / 2);
        position = "Short";
        System.out.println("📉 SHORT 진입 at " + entryPrice);
    }

    private boolean shouldClosePosition(double price) {
        if ("Long".equals(position)) {
            double profitRate = (price - entryPrice) / entryPrice * LEVERAGE;
            return profitRate >= TAKE_PROFIT_RATE || profitRate <= -STOP_LOSS_RATE;
        } else if ("Short".equals(position)) {
            double profitRate = (entryPrice - price) / entryPrice * LEVERAGE;
            return profitRate >= TAKE_PROFIT_RATE || profitRate <= -STOP_LOSS_RATE;
        }
        return false;
    }

    private void closePosition(double price) {
        double profitRate = 0.0;
        if ("Long".equals(position)) {
            profitRate = (price - entryPrice) / entryPrice * LEVERAGE;
        } else if ("Short".equals(position)) {
            profitRate = (entryPrice - price) / entryPrice * LEVERAGE;
        }
        balance = balance * (1 + profitRate - ENTRY_FEE_RATE);
        totalTrades++;
        if (profitRate > 0) {
            winTrades++;
        }
        peakBalance = Math.max(peakBalance, balance);
        maxDrawdown = Math.min(maxDrawdown, (balance - peakBalance) / peakBalance);
        System.out.println("✅ 포지션 종료. 현재 잔고: " + balance);
        position = "None";
    }

    private void printResult() {
        System.out.println("\n========= 📊 BACKTEST RESULT 📊 =========");
        System.out.println("최종 잔고: " + balance);
        System.out.println("총 트레이드 수: " + totalTrades);
        System.out.println("승률: " + (totalTrades > 0 ? (winTrades * 100.0 / totalTrades) : 0) + "%");
        System.out.println("최대 낙폭 (Max Drawdown): " + (maxDrawdown * 100.0) + "%");
        System.out.println("==========================================\n");
    }

    // 15분봉 추세 판별 (EMA, STC, SSL 이용)
    private String determineTrend(List<Double> closes) {
        double ema9 = calculateEMA(closes, 9);
        double ema25 = calculateEMA(closes, 25);
        String stc1 = Indicator.getSTC(closes, 80, 27, 50).block();
        String stc2 = Indicator.getSTC(closes, 32, 26, 50).block();
        boolean sslUptrend = isSSLUptrend(closes);

        if (ema9 > ema25 && "Long".equals(stc1) && "Long".equals(stc2) && sslUptrend) {
            return "Long";
        } else if (ema9 < ema25 && "Short".equals(stc1) && "Short".equals(stc2) && !sslUptrend) {
            return "Short";
        }
        return "SIDEWAYS";
    }

    private double calculateEMA(List<Double> prices, int period) {
        double k = 2.0 / (period + 1);
        double ema = prices.get(0);
        for (int i = 1; i < prices.size(); i++) {
            ema = prices.get(i) * k + ema * (1 - k);
        }
        return ema;
    }

    private boolean isSSLUptrend(List<Double> closes) {
        if (closes.size() < 60) return false;
        int n = closes.size();
        return closes.get(n - 5) < closes.get(n - 4)
                && closes.get(n - 4) < closes.get(n - 3)
                && closes.get(n - 3) < closes.get(n - 2)
                && closes.get(n - 2) < closes.get(n - 1);
    }
}
