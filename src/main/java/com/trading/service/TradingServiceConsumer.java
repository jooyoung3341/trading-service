package com.trading.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import com.trading.service.DB.History;
import com.trading.service.DB.HistoryService;
import com.trading.service.common.Indicator;
import com.trading.service.common.TradingUtil;
import com.trading.service.controller.WebController;
import com.trading.service.model.Candle;
import com.trading.service.model.Candles;
import com.trading.service.model.EnumType;
import com.trading.service.model.QqeResult;
import com.trading.service.model.Ticker;
import com.trading.service.service.BinanceRestService;
import com.trading.service.service.BinanceService;
import com.trading.service.service.RedisService;
import com.trading.service.service.TeleService;
import com.trading.service.service.TradingService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class TradingServiceConsumer implements CommandLineRunner{

	@Autowired
	private BinanceRestService restService;
	@Autowired
	private RedisService redisService;
	@Autowired
	private BinanceService binanceService;
	@Autowired
	private Indicator indicator;
	@Autowired
	private TradingUtil util;
	@Autowired
	private TradingService tradingService;
	@Autowired
	private HistoryService historyService;
	@Autowired
	private WebController t;
	@Autowired
	private TeleService teleService;
	int period9 = 9;
	int period25 = 25;
	int period99 = 99;
	
	@Override
	public void run(String... args) throws Exception {
		System.out.println("실행 됨 ");

		//long   = 롱
		//short  = 숏
		//none = 보합
		
		/* 해야 할거
		 * 15분봉 롱 숏 보합 구함, 3개심볼이 보합일경우 스킵
		 * 하나라도 롱 또는 숏일 경우 그 심볼만 가지고 폴링하기 여기까지 하자 
		 * 그리고 롱숏 어떻게 잡을지 정해보자고
		*/
		
		/*
		 * 레디스에서 내 포지션 확인
			없을경우와 -> 있을경우
			없을 경우에는 바로 포지션 잡지 않기 위해 한번 꼬는 로직 추가
			
			포지션에 대한 정보 db에 저장
			
			포지션에 대한 시각화 웹으로 출력
		*/
		
		//포지션 on = true, off = false
		AtomicBoolean isPosition = new AtomicBoolean(false);

		//추세를 알기위한 15분
		AtomicReference<String> is15Trand = new AtomicReference<>();
		AtomicReference<Map<String, Object>> teleMap = new AtomicReference<>();
		//추세를 알기위한 15분
		AtomicReference<String> is5Trand = new AtomicReference<>();
		//추세를 알기위한 1분
		AtomicReference<String> is1Trand = new AtomicReference<>();
		//감시중인 심볼
		AtomicReference<String> targetSymbol = new AtomicReference<>();
		//매매중인 포지션 DB PK 값
		AtomicLong dbPk = new AtomicLong(0);
		//포지션 진입 가격
		AtomicReference<Double> isPrice = new AtomicReference<>();
		
		AtomicInteger timeSeq = new AtomicInteger(0);

		AtomicReference<String> teleText = new AtomicReference<>();
		
		//process(is1Trand, is5Trand, is15Trand, isPosition, dbPk, timeSeq, targetSymbol, isPrice);
		//처음 일 경우 신호가 오더라도 매매X
		//true = 처음 , false = 이후
		AtomicBoolean isStart = new AtomicBoolean(true);
		//teleProcess(isStart);
		//aa().subscribe();
	}

	
	public void teleProcess(AtomicBoolean isStart) {
		System.out.println("teleProcess 최초 실행");
		Flux.defer(() -> Flux.interval(Duration.ofSeconds(60))
				.flatMap(tick -> redisService.getTradingSymbolList(EnumType.TradingSymbol.value())
						.flatMapMany(Flux::fromIterable)
						.flatMap(symbol -> restService.getPrice(symbol)
								.flatMap(price -> tradingService.trandType(symbol, EnumType.m15.value())
										.flatMap(m15_trand -> tradingService.trandType(symbol, EnumType.m5.value())
												.flatMap(m5_trand -> {
													System.out.println("teleProcess start : " + symbol);
													if(isStart.get()) {
														//처음 실행이면 
														return saveTele(symbol, m5_trand, m15_trand)
																.flatMap(r -> {
																			return Mono.empty();
																		});
													}
													//처음실행 아니면
													return redisService.getValue(EnumType.m5_tele.value()+symbol)
															.flatMap(m5_tele -> redisService.getValue(EnumType.m15_tele.value()+symbol)
																	.flatMap(m15_tele -> {
																		if(m5_tele.equals("start") && m15_tele.equals("start")) {
																			//중간에 추가된 심볼
																			return saveTele(symbol, m5_trand, m15_trand)
																					.flatMap(r -> {
																								return Mono.empty();
																							});
																		}
																		String teleText = "";
																		if(!m5_tele.equals(m5_trand)) {
																			//추세가 바뀌면 텔레그램 전송
																			teleText = teleStr(m5_tele, m5_trand, symbol, String.valueOf(price), "5분봉");
																		}
																		if(!m15_tele.equals(m15_trand)) {
																			//추세가 바뀌면 텔레그램 전송
																			if(!teleText.equals("")) {
																				teleText += "\n";
																			}
																			teleText += teleStr(m15_tele, m15_trand, symbol, String.valueOf(price), "15분봉");
																		}
																		
																		if(!teleText.equals("")) {
																			
																			//텔레그램 전송
																			return teleService.sendMessage(teleText)
																					.flatMap(r -> saveTele(symbol, m5_tele, m15_trand)
																							.flatMap(re -> {
																								return Mono.empty();
																							})
																					);
																		}
																		return Mono.empty();
																	})
																);
												})
											)
										)
								)
						.collectList()
						.doOnSuccess(list ->{
							if(isStart.get()) {
								isStart.set(false);
							}
						})
						.then()
						)
				).subscribe();
	}
	
	public String teleStr(String asisTrand, String toTrand, String symbol, String price, String time) {
		return symbol + " " + time + " 추세전환 ! " + asisTrand + " -> " + toTrand + " 현재 가격 : " + price;
	}
	public Mono<Boolean> saveTele(String symbol, String m5_trand, String m15_trand){
		return Mono.defer(() -> redisService.saveValue(EnumType.m15_tele.value()+symbol, m15_trand)
				.flatMap(r -> redisService.saveValue(EnumType.m5_tele.value()+symbol, m5_trand)
						.flatMap(re -> {
							return Mono.just(re);
						})
				)
		);
	}
	
	
	/*
	 * 15분봉 ema9, ema25 정배열, 역배열 구하기 -> 15분봉 ssl 선이 현재가격 위치(정배열이면 위에 위치해야하고 역배열이면 아래에 위치해야 추세확인)
		-> 1분봉 ema9, ema25 ssl최근데이터5개를 구해서 15분봉과 반대추세인지 확인(1분봉 ema25아래에 있을경우로 변경?)
		 -> 반대추세였다가 다시 15분봉이랑 같은추세로 전환시 신호
		
		15분봉 ema, ssl 위 아래로만?
		저러면 고점에서 매수할 수 도 있어서 5분봉 ssl선을 구하는것도 좋은 방법일듯 
		
		stc, qqe 모드 추가해서
		isStart를 넣어서 이게 true면 처음이니깐 지금이 롱추세면 롱진입x 그리고 false로 바꾸고 숏에만 진입하게끔
		
		
		매물대 15분봉도 같이 구현
	*/
	/*public void process1(AtomicReference<String> is1Trand, AtomicReference<String> is5Trand, AtomicReference<String> is15Trand,
									AtomicBoolean isPosition, AtomicLong dbPk, AtomicInteger timeSeq, AtomicReference<String> targetSymbol,
									AtomicReference<Double> isPrice) {
		Flux.defer(() -> Flux.interval(Duration.ofSeconds(10))
				.flatMap(tick -> {
					if(!isPosition.get()) {
						//포지션 없음
						return redisService.getTradingSymbolList(EnumType.autoSymbol.value())
								.flatMapMany(Flux::fromIterable)
								.flatMap(symbol -> restService.getPrice(symbol)
									.flatMap(price -> restService.getCandles(symbol, EnumType.m15.value(), Integer.parseInt(EnumType.candle.value()))
										.flatMap(m15_list -> {
											Candles m15_candles = new Candles().setCandles(m15_list);
											List<Double> m15_close = m15_candles.getCloses().subList(0, (m15_candles.getCloses().size() -1));
											
											double m15_ema25 = indicator.ema(m15_close, 25);
											double m15_ema9 = indicator.ema(m15_close, 9);
											String m15_trand = "";
											if(m15_ema25 > m15_ema9) {
												//숏 역배
												m15_trand = EnumType.Short.value();
												return Mono.just(EnumType.Short.name());
											}else if(m15_ema25 < m15_ema9) {
												//롱 정배
												m15_trand = EnumType.Long.value();
												return Mono.just(EnumType.Long.name());
											}
											
											List<Double> sslData = indicator.ssl(m15_candles.getHigh(), m15_candles.getLow(), m15_candles.getCloses(), 60);
											int sslData_size = (sslData.size()-1);
											List<Double> ssl = sslData.subList((sslData_size-10), sslData_size);
											double sslDouble = ssl.get(ssl.size()-1);
											if(price > sslDouble) {
												//롱
												if(!m15_trand.equals(EnumType.Long.value())) {
													//추세가 다르면 리턴
													return Mono.empty();
												}
											}else {
												//숏
												if(!m15_trand.equals(EnumType.Short.value())) {
													//추세가 다르면 리턴
													return Mono.empty();
												}
											}
											return restService.getCandles(symbol, EnumType.m5.value(), Integer.parseInt(EnumType.candle.value()))
													.flatMap(m5_list -> {
														Candles m5_candles = new Candles().setCandles(m5_list);
														List<Double> m5_close = m5_candles.getCloses().subList(0, (m5_candles.getCloses().size() -1));
														
														List<Double> sslData = indicator.ssl(m15_candles.getHigh(), m15_candles.getLow(), m15_candles.getCloses(), 60);
														int sslData_size = (sslData.size()-1);
														List<Double> ssl = sslData.subList((sslData_size-10), sslData_size);
													});
													
											
									});
								)
							);
					}else {
						//포지션 있음
					}
				})
		);
	}*/
	
	public void process(AtomicReference<String> is1Trand, AtomicReference<String> is5Trand, AtomicReference<String> is15Trand,
									AtomicBoolean isPosition, AtomicLong dbPk, AtomicInteger timeSeq, AtomicReference<String> targetSymbol,
									AtomicReference<Double> isPrice) {
		Flux.defer(() -> Flux.interval(Duration.ofSeconds(10))
		.flatMap(tick -> {
			System.out.println("isPosition : " + isPosition.get());
			if(!isPosition.get()) {
				//포지션 없음
				//return Mono.empty();
				return redisService.getTradingSymbolList(EnumType.autoSymbol.value())
						.flatMapMany(Flux::fromIterable)
						.flatMap(symbol -> tradingService.trandCandle(symbol, EnumType.m1.value())
								.flatMap(m1_trand -> tradingService.trandCandle(symbol, EnumType.m5.value())
										.flatMap(m5_trand -> tradingService.trandCandle(symbol, EnumType.m15.value())
												.flatMap(m15_trand -> {
													System.out.println("m15_trand : " + m15_trand);
													if(m15_trand.equals(EnumType.None.value())) {
	                                    				//횡보면 리턴
	                                    				return Mono.empty();
	                                    			}
													if(is15Trand.get() != null) {
														if(!is15Trand.get().equals(m15_trand) && !is15Trand.get().equals(null)) {
		                                    				//추세가 전환되면 다시 처음부터
		                                    				System.out.println("추세 전환");
	                        								is15Trand.set(null);
		                                    				is5Trand.set(null);
		                                    				is1Trand.set(null);
		                                    				return Mono.empty();
		                                    			}
													}
													
                        							is1Trand.set(m1_trand);
                        							is5Trand.set(m5_trand);
	                                    			is15Trand.set(m15_trand);
	                                    			System.out.println("m1_trand : " + m1_trand);
	                                    			System.out.println("m5_trand : " + m5_trand);
	                                    			System.out.println("m15_trand : " + m15_trand);
	                                    			
	                                    			return redisAutoPositionOpen(symbol, is15Trand, is5Trand, is1Trand, dbPk, isPosition, targetSymbol, isPrice).then();
												})
												)
										)
								);
			}else {
				//포지션 있음
				//포지션 종료 로직 만들어야함. 그리고 손절라인 만들어야함 0.7%정도? 알트는 좀더 크게잡고 
				return historyService.findByPk(dbPk.get())
						.flatMap(h -> restService.getPrice(targetSymbol.get())
									.flatMap(price -> {
										boolean isPosi = false;
										History his = new History();
										if(h.getTrand().equals(EnumType.Long.value())) {
											//long
											if(price < util.minusPercent(isPrice.get(), 0.7)) {
												//stop
												isPosi = true;
											}else if(price > util.plusPercent(isPrice.get(), 0.7)) {
												//take
												isPosi = true;
											}
										}else if(h.getTrand().equals(EnumType.Short.value())) {
											//short
											if(price < util.minusPercent(isPrice.get(), 0.7)) {
												//take
												isPosi = true;
											}else if(price > util.plusPercent(isPrice.get(), 0.7)) {
												//stop
												isPosi = true;
											}
											
											System.out.println("isPosi : " + isPosi);
											if(isPosi == false) {
												return Mono.empty();
											}
										}
										LocalDateTime now = LocalDateTime.now();
										his.setTimeSeq(dbPk.get());
										his.setClosePrice(String.valueOf(price));
										his.setCloseTime(now);
										his.setIsing(EnumType.o.value());
										his.setPercent(String.valueOf(util.calculatePercentage(isPrice.get(), price)));
										return historyService.save(his)
												.flatMap(r -> {
													System.out.println("포지션 종료");
													isPosition.set(false);
													targetSymbol.set("");
													dbPk.set(0);
													isPrice.set(0.0);
													return Mono.empty();
												});
										
									})
						);
			}
		})
	).subscribe();
}
	
	

	//15분봉으로만 추세를 보고
	//5분봉 역추세일 경우 15분봉 ema에 진입?
	public Mono<Void> redisAutoPositionOpen(String symbol, AtomicReference<String> is15Trand, AtomicReference<String> is5Trand, AtomicReference<String> is1Trand
																	,AtomicLong dbPk, AtomicBoolean isPosition, AtomicReference<String> targetSymbol, AtomicReference<Double> isPrice){
		return Mono.defer(() -> restService.getCandles(symbol, EnumType.m5.value(), (99+11))
				.flatMap(m5_list -> restService.getCandles(symbol, EnumType.m15.value(), (99+11))
						.flatMap(m15_list -> restService.getCandles(symbol, EnumType.m1.value(), (99+11))
								.flatMap(m1_list -> restService.getPrice(symbol)
										.flatMap(price -> {
											Candles m5_candles = new Candles().setCandles(m5_list);
											List<Double> m5_close = m5_candles.getCloses().subList(0, (m5_candles.getCloses().size() -1));
											Candles m15_candles = new Candles().setCandles(m15_list);
											List<Double> m15_close = m15_candles.getCloses().subList(0, (m15_candles.getCloses().size() -1));
											//Candles m1_candles = new Candles().setCandles(m1_list);
											//List<Double> m1_close = m1_candles.getCloses().subList(0, (m1_candles.getCloses().size() -1));
											
											
											double m5_ema99 = indicator.ema(m5_close, 99);
											double m15_ema25 = indicator.ema(m15_close, 25);
											
											if(is15Trand.get().equals(EnumType.Long.value())) {
												System.out.println("롱 대기");
												//롱
												if(!is1Trand.get().equals(is15Trand)) {
													//반대일경우에만 포지션 진입
													if(m5_ema99 < price || m15_ema25 < price) {
														//롱 진입
														System.out.println("롱 진입");
														History h = new History();
														h.setTrand(EnumType.Long.value());
														isPrice.set(price);
														return positionOpen(dbPk, targetSymbol, isPosition, symbol, price, h).then();
													}
												}
												
											}else if(is15Trand.get().equals(EnumType.Short.value())){
												//숏
												System.out.println("숏 대기");
												if(!is1Trand.get().equals(is15Trand)) {
													//반대일경우에만 포지션 진입
													if(m5_ema99 > price || m15_ema25 > price) {
														//숏 진입
														System.out.println("숏 진입");
														History h = new History();
														h.setTrand(EnumType.Short.value());
														isPrice.set(price);
														return positionOpen(dbPk, targetSymbol, isPosition, symbol, price, h).then();
													}
												}
											}
											return Mono.empty();
										})
						)
					)
				)
		);
	}
	
	public Mono<Void> positionOpen(AtomicLong dbPk, AtomicReference<String> targetSymbol, AtomicBoolean isPosition, String symbol, 
													double price, History h){
		return Mono.defer(() -> {
			LocalDateTime now = LocalDateTime.now();
			h.setIsing(EnumType.x.value());
			h.setSymbol(symbol);
			h.setOpenPrice(String.valueOf(price));
			h.setOpenTime(now);
			h.setTimeSeq(now.atZone(ZoneId.systemDefault()).toEpochSecond());
			h.setTrand(EnumType.Short.value());
			
			return historyService.save(h)
					.flatMap(his -> {
						System.out.println(h.getTrand() + "  진 입 ");
						dbPk.set( h.getTimeSeq());
						targetSymbol.set(symbol);
						isPosition.set(true);
						return Mono.empty();
					});
			
		});
		
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
					//System.out.println("ema : " + ema99);
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
					
					//System.out.println("isBuySignal : " + isBuySignal);
					//System.out.println("isSellSignal : " + isSellSignal);
					
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
