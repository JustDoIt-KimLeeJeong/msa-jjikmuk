package com.jjikmuk.execution_service.domain.service;

import com.jjikmuk.execution_service.domain.model.Fill;
import com.jjikmuk.execution_service.domain.model.Order;
import com.jjikmuk.execution_service.domain.model.value.OrderId;
import com.jjikmuk.execution_service.domain.model.value.Symbol;
import com.jjikmuk.execution_service.domain.port.ExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class MatchingEngine {

    private final ExecutionRepository executionRepository;

    /**
     * 두 주문(신규 주문, 기존 오더북 주문)을 받아 매칭을 시도하고, 체결이 성사되면 체결 정보(Fill)를 반환합니다.
     *
     * @param incomingOrder 신규 접수된 주문
     * @param openOrder     오더북에 있던 반대 주문
     * @param now           체결 시각
     * @return 체결 정보를 담은 Optional 객체. 체결되지 않으면 Optional.empty()
     */
    public Optional<Fill> match(Order incomingOrder, Order openOrder, Instant now) {
        log.debug("매칭 시도 중 - 신규 주문: {}, 오더북 주문: {}", incomingOrder, openOrder);
        // 0. 심볼이 다르면 매칭 실패
        if (!incomingOrder.getSymbol().equals(openOrder.getSymbol())) {
            log.debug("매칭 실패: 심볼 불일치 (신규: {}, 오더북: {})", incomingOrder.getSymbol(), openOrder.getSymbol());
            return Optional.empty();
        }

        // 1. 체결 가능한 수량이 없으면 매칭 실패
        if (incomingOrder.getLeavesQty() == 0 || openOrder.getLeavesQty() == 0) {
            log.debug("매칭 실패: 잔여 수량 0 (신규: {}, 오더북: {})", incomingOrder.getLeavesQty(), openOrder.getLeavesQty());
            return Optional.empty();
        }

        // 2. 가격 조건이 맞지 않으면 매칭 실패 (매수 희망가 >= 매도 희망가)
        if (!incomingOrder.crosses(openOrder.getPrice())) {
            log.debug("매칭 실패: 가격 조건 불일치 (신규 가격: {}, 오더북 가격: {})", incomingOrder.getPrice(), openOrder.getPrice());
            return Optional.empty();
        }

        // 3. 체결 수량 및 가격 결정
        long tradeQty = Math.min(incomingOrder.getLeavesQty(), openOrder.getLeavesQty());
        // 체결 가격은 오더북에 원래 있던 주문(maker)의 가격을 따름
        BigDecimal tradePrice = openOrder.getPrice();

        // 4. 각 주문의 남은 수량(잔량) 업데이트
        incomingOrder.setLeavesQty(incomingOrder.getLeavesQty() - tradeQty);
        openOrder.setLeavesQty(openOrder.getLeavesQty() - tradeQty);

        // 5. 체결 정보(Fill) 생성 및 반환
        Fill fill = new Fill(openOrder.getOrderId(), tradePrice, tradeQty, now);
        log.debug("매칭 성공: 체결 수량={}, 체결 가격={}, Fill={}", tradeQty, tradePrice, fill);
        return Optional.of(fill);
    }

    /**
     * Tick 정보를 기반으로 오더북의 주문들을 체결 시도합니다.
     *
     * @param symbol  종목 심볼
     * @param bidp1   최우선 매수호가
     * @param askp1   최우선 매도호가
     * @param now     현재 시각
     * @return 체결된 Fill 목록
     */
    public List<Fill> processTick(Symbol symbol, BigDecimal bidp1, BigDecimal askp1, Instant now) {
        log.debug("틱 처리 중 - 심볼: {}, 최우선 매수호가: {}, 최우선 매도호가: {}", symbol, bidp1, askp1);
        List<Fill> fills = new ArrayList<>();

        // 1. 체결 가능한 매수 주문들을 조회 (매수 희망가 >= 시장가 매도호가)
        List<Order> matchingBuyOrders = executionRepository.findMatchingBuyOrders(symbol, askp1);
        log.debug("매칭되는 매수 주문 {}건 발견.", matchingBuyOrders.size());
        for (Order buyOrder : matchingBuyOrders) {
            // 가상의 매도 주문 생성 (시장가 매도 주문으로 간주)
            Order syntheticSellOrder = Order.builder()
                .orderId(new OrderId("SYNTHETIC-SELL-" + buyOrder.getOrderId().value() + "-" + now.toEpochMilli())) // 고유 ID 생성
                .symbol(symbol)
                .side(Order.Side.SELL)
                .type(Order.Type.MARKET) // 시장가 주문으로 간주
                .price(askp1) // 체결 가격은 askp1
                .origQty(buyOrder.getLeavesQty()) // 기존 주문의 잔량을 원본 수량으로
                .leavesQty(buyOrder.getLeavesQty()) // 기존 주문의 잔량을 남은 수량으로
                .tif(Order.Tif.IOC) // 즉시 체결 또는 취소
                .arrivalSeq(buyOrder.getArrivalSeq()) // 기존 arrivalSeq 사용
                .createdAt(now) // 생성 시간
                .build();
            log.debug("가상 매도 주문 생성: {}", syntheticSellOrder);

            Optional<Fill> fillOpt = match(buyOrder, syntheticSellOrder, now);
            fillOpt.ifPresent(fills::add);
        }

        // 2. 체결 가능한 매도 주문들을 조회 (매도 희망가 <= 시장가 매수호가)
        List<Order> matchingSellOrders = executionRepository.findMatchingSellOrders(symbol, bidp1);
        log.debug("매칭되는 매도 주문 {}건 발견.", matchingSellOrders.size());
        for (Order sellOrder : matchingSellOrders) {
            // 가상의 매수 주문 생성 (시장가 매수 주문으로 간주)
            Order syntheticBuyOrder = Order.builder()
                .orderId(new OrderId("SYNTHETIC-BUY-" + sellOrder.getOrderId().value() + "-" + now.toEpochMilli())) // 고유 ID 생성
                .symbol(symbol)
                .side(Order.Side.BUY)
                .type(Order.Type.MARKET)
                .price(bidp1) // 체결 가격은 bidp1
                .origQty(sellOrder.getLeavesQty())
                .leavesQty(sellOrder.getLeavesQty())
                .tif(Order.Tif.IOC)
                .arrivalSeq(sellOrder.getArrivalSeq())
                .createdAt(now)
                .build();
            log.debug("가상 매수 주문 생성: {}", syntheticBuyOrder);

            Optional<Fill> fillOpt = match(sellOrder, syntheticBuyOrder, now);
            fillOpt.ifPresent(fill -> {
                fills.add(fill);
                log.debug("Fill 목록에 추가: {}", fill);
            });
        }

        return fills;
    }
}
