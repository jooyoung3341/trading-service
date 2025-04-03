package com.trading.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import com.trading.service.indicator.Indicator;
import com.trading.service.model.Candle;
import com.trading.service.model.QqeResult;
import com.trading.service.service.BinanceRestService;

import reactor.core.publisher.Mono;

@Service
public class TradingServiceConsumer implements CommandLineRunner{

	@Autowired
	private BinanceRestService restService;
	@Autowired
	private Indicator indicator;
	@Override
	public void run(String... args) throws Exception {
		System.out.println("실행 됨 ");
		tt().subscribe();
		
	}
	
	public Mono<Void> tt(){
		return Mono.defer(() -> restService.getCandles("BTCUSDT", "15m", 100))
				.flatMap(list -> {
					List<Double> closes = list.stream()
		                    .map(Candle::getClose)
		                    .toList();
					List<Double> high = list.stream()
		                    .map(Candle::getHigh)
		                    .toList();
					List<Double> low = list.stream()
		                    .map(Candle::getLow)
		                    .toList();
					double ema99 = indicator.ema(closes, 99);
					System.out.println("ema : " + ema99);
					QqeResult primaryQqe = indicator.qqe(closes, 12, 10, 6.0);
					QqeResult secondQqe = indicator.qqe(closes, 12, 10, 1.61);
					System.out.println("primaryQqe trendLine : " + primaryQqe.getTrendLine());
					System.out.println("primaryQqe smoothed Line : " + primaryQqe.getSmoothedRsi());
					
					System.out.println("secondQqe trendLine : " + secondQqe.getTrendLine());
					System.out.println("secondQqe smoothed Line : " + secondQqe.getSmoothedRsi());
					double thresholdSecondary = 6.0;

					// RSI 강도 판단 (보조 지표)
					boolean secondaryUp = secondQqe.getSmoothedRsi() - 50 > thresholdSecondary;
					boolean secondaryDown = secondQqe.getSmoothedRsi() - 50 < -thresholdSecondary;
					System.out.println("secondaryUp : " + secondaryUp);
					System.out.println("secondaryDown : " + secondaryDown);
					// 볼린저 밴드 계산
					double basis = indicator.sma(closes, 50);
					double dev = indicator.stdDev(closes, 50);
					double bollingerUpper = basis + dev * 0.35;
					double bollingerLower = basis - dev * 0.35;
					
					// 추세 강도 판단 (메인 지표)
					boolean primaryUp = primaryQqe.getSmoothedRsi() - 50 > bollingerUpper;
					boolean primaryDown = primaryQqe.getSmoothedRsi() - 50 < bollingerLower;
					//primaryQqe.getSmoothedRsi() - 50  이게 막대지표 인듯
					System.out.println("primaryQqe.getSmoothedRsi() - 50 : " + (primaryQqe.getSmoothedRsi() - 50));
					System.out.println("secondQqe.getSmoothedRsi() - 50 : " + (secondQqe.getSmoothedRsi() - 50));
					
					System.out.println("bollingerUpper : " + bollingerUpper);
					System.out.println("bollingerLower : " + bollingerLower);
					// 최종 시그널
					boolean isBuySignal = secondaryUp && primaryUp;
					boolean isSellSignal = secondaryDown && primaryDown;
					
					System.out.println("isBuySignal : " + isBuySignal);
					System.out.println("isSellSignal : " + isSellSignal);
					
					List<Double> ssl = indicator.ssl(high, low, closes, 60);
					
					int i = 0;
					for (double s : ssl) {
						System.out.println(i + " / ssl : " + s);
						i++;
					}
					/*
					 * 1분봉, 5분봉 위주로 데이터를 구함.
					 * 단기추세는 5분봉으로 > 롱추세일 떄 롱을 익절 하고 바로 롱포지션 또 안잡기위해 1분봉 추세변환까지 대기, 5분봉 99선과 현재 가격 간격 확인
					 * 추세로 포지션을 잡을때는 1분봉 99선, 5분봉 25선 ( 롱일 떄는 1분봉이 99선보다 위에있을 떄 근처오거나 닿으면?? 
					 * 	qqe모드는 15분봉 기준으로 추세를 보는게 나을려나?
					 * 오히려 qqe 5분봉으로 구하고 -3~3 사이에는 포지션을 안잡게 하는게 더 나을지도?
					*/					
					
					return Mono.empty();
				});
	}
}
