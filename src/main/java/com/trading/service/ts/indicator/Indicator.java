package com.trading.service.ts.indicator;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.trading.service.ts.model.QqeResult;

@Component
public class Indicator {
	

	// Exponential Moving Average (EMA)
	public static double ema(List<Double> data, int period) {
		double alpha = 2.0 / (period + 1.0);
		double emaValue = data.get(0); // 초기값 설정
		for (int i = 1; i < data.size(); i++) {
			emaValue = alpha * data.get(i) + (1 - alpha) * emaValue;
		}
		return emaValue;
	}

	public static List<Double> emaList(List<Double> data, int period) {
	    List<Double> emaList = new ArrayList<>();
	    if (data == null || data.size() < period) return emaList;

	    double multiplier = 2.0 / (period + 1);
	    double ema = data.get(0); // 초기값 = 첫 데이터 (혹은 sma로 설정 가능)
	    emaList.add(ema);

	    for (int i = 1; i < data.size(); i++) {
	        ema = (data.get(i) * multiplier) + (ema * (1 - multiplier));
	        emaList.add(ema);
	    }

	    return emaList;
	}
	
	// Weighted Moving Average (WMA)
	public static double wma(List<Double> data, int period) {
		int size = data.size();
   	 
        double weightedSum = 0.0;
        if(data.size() <= 1) {
        	for (int i = 0; i < period; i++) {
        		weightedSum += data.get(0)*(i+1);
			}
       	 }else {
       		 for (int i = 0; i < period; i++) {
       			 weightedSum += data.get(size - period + i) * (i + 1);
       		 }
       	 }
        
        double sumOfWeights = (period * (period + 1)) / 2.0;
        
        return weightedSum / sumOfWeights;
   }

	// Hull Moving Average (HMA)
	public static double hma(List<Double> data, int period) {

		List<Double> dataArrHma = new ArrayList<>();
		for (int i = 0; i < period; i++) {
			dataArrHma.add(data.get(data.size() - period + i));
		}

		double wmaHalfPeriod = wma(dataArrHma, period / 2);
		double wmaPeriod = wma(dataArrHma, period);

		List<Double> dataArr = new ArrayList<>();
		dataArr.add(2 * wmaHalfPeriod - wmaPeriod);

		return wma(dataArr, (int) Math.sqrt(period));
	}

	 public static List<Double> rsi(List<Double> closes, int period) {
	        List<Double> rsiList = new ArrayList<>();
	        if (closes == null || closes.size() <= period) return rsiList;

	        double gain = 0, loss = 0;

	        // 초기 평균 gain/loss 계산
	        for (int i = 1; i <= period; i++) {
	            double change = closes.get(i) - closes.get(i - 1);
	            if (change > 0) gain += change;
	            else loss -= change;
	        }

	        gain /= period;
	        loss /= period;

	        double rs = loss == 0 ? 100 : gain / loss;
	        rsiList.add(100 - (100 / (1 + rs))); // 첫 RSI

	        // 이후 RSI 계산
	        for (int i = period + 1; i < closes.size(); i++) {
	            double change = closes.get(i) - closes.get(i - 1);
	            double currentGain = change > 0 ? change : 0;
	            double currentLoss = change < 0 ? -change : 0;

	            gain = ((gain * (period - 1)) + currentGain) / period;
	            loss = ((loss * (period - 1)) + currentLoss) / period;

	            rs = loss == 0 ? 100 : gain / loss;
	            double rsi = 100 - (100 / (1 + rs));
	            rsiList.add(rsi);
	        }

	        return rsiList;
	    }
	 
//========================================================================
	 
	// True Range
	public static double trueRange(double high, double low, double close) {
		return Math.max(Math.max(Math.abs(high - low), Math.abs(high - close)), Math.abs(low - close));
	}

	// SSL Upper Band
	public static double sslUpperk(List<Double> close, List<Double> high, List<Double> low, int period) {
		double keltma = hma(close, period);

		List<Double> rangeArr = new ArrayList<>();
		for (int i = 0; i < period; i++) {
			double range = trueRange(high.get(i), low.get(i), close.get(i));
			rangeArr.add(range);
		}
		double rangema = ema(rangeArr, period);
		return keltma + rangema * 0.2;
	}

	// SSL Lower Band
	public static double sslLowerk(List<Double> close, List<Double> high, List<Double> low, int period) {
		double keltma = hma(close, period);
		List<Double> rangeArr = new ArrayList<>();
		for (int i = 0; i < period; i++) {
			double range = trueRange(high.get(i), low.get(i), close.get(i));
			rangeArr.add(range);
		}
		double rangema = ema(rangeArr, period);       
		return keltma - rangema * 0.2;
	}
	
	 public static QqeResult calculateQqe(
	            List<Double> close,
	            int rsiLength,
	            int smoothingLength,
	            double qqeFactor
	    ) {
	        List<Double> rsi = rsi(close, rsiLength);
	        List<Double> smoothedRsi = emaList(rsi, smoothingLength);

	        List<Double> atrRsi = new ArrayList<>();
	        for (int i = 1; i < smoothedRsi.size(); i++) {
	            atrRsi.add(Math.abs(smoothedRsi.get(i) - smoothedRsi.get(i - 1)));
	        }

	        List<Double> smoothedAtr = emaList(atrRsi, rsiLength * 2 - 1);
	        List<Double> dynAtr = smoothedAtr.stream().map(val -> val * qqeFactor).toList();

	        double longBand = 0;
	        double shortBand = 0;
	        int trend = 0;
	        double trendLine = 0;

	        for (int i = 1; i < dynAtr.size(); i++) {
	            double rsiVal = smoothedRsi.get(i + 1);
	            double delta = dynAtr.get(i);

	            double newLong = rsiVal - delta;
	            double newShort = rsiVal + delta;

	            longBand = rsiVal > longBand ? Math.max(longBand, newLong) : newLong;
	            shortBand = rsiVal < shortBand ? Math.min(shortBand, newShort) : newShort;

	            if (rsiVal > shortBand) trend = 1;
	            else if (rsiVal < longBand) trend = -1;

	            trendLine = trend == 1 ? longBand : shortBand;
	        }

	        QqeResult result = new QqeResult();
	        result.trendLine = trendLine;
	        result.smoothedRsi = smoothedRsi.get(smoothedRsi.size() - 1);
	        return result;
	    }
}
