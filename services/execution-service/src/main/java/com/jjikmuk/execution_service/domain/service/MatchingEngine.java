package com.jjikmuk.execution_service.domain.service;

import com.jjikmuk.execution_service.domain.model.Fill;
import com.jjikmuk.execution_service.domain.model.Order;
import com.jjikmuk.execution_service.domain.model.value.OrderId;
import com.jjikmuk.execution_service.domain.model.value.Symbol;
import com.jjikmuk.execution_service.domain.port.ExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
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
        // 0. 심볼이 다르면 매칭 실패
        if (!incomingOrder.getSymbol().equals(openOrder.getSymbol())) {
            return Optional.empty();
        }

        // 1. 체결 가능한 수량이 없으면 매칭 실패
        if (incomingOrder.getLeavesQty() == 0 || openOrder.getLeavesQty() == 0) {
            return Optional.empty();
        }

        // 2. 가격 조건이 맞지 않으면 매칭 실패 (매수 희망가 >= 매도 희망가)
        if (!incomingOrder.crosses(openOrder.getPrice())) {
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
        return Optional.of(new Fill(openOrder.getOrderId(), tradePrice, tradeQty, now));
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
        List<Fill> fills = new ArrayList<>();

        // 1. 해당 심볼의 모든 오픈 주문을 가져옵니다.
        List<Order> openOrders = executionRepository.findAllOpenOrdersBySymbol(symbol);

        for (Order openOrder : openOrders) {
            // 2. 매수 주문 처리: askp1 가격으로 체결 가능한지 확인
            if (openOrder.getSide() == Order.Side.BUY && openOrder.getPrice().compareTo(askp1) >= 0) {
                // 가상의 매도 주문 생성 (시장가 매도 주문으로 간주)
                Order syntheticSellOrder = Order.builder()
                        .orderId(new OrderId("SYNTHETIC-SELL-" + openOrder.getOrderId().value() + "-" + now.toEpochMilli())) // 고유 ID 생성
                        .symbol(symbol)
                        .side(Order.Side.SELL)
                        .type(Order.Type.MARKET) // 시장가 주문으로 간주
                        .price(askp1) // 체결 가격은 askp1
                        .origQty(openOrder.getLeavesQty()) // 기존 주문의 잔량을 원본 수량으로
                        .leavesQty(openOrder.getLeavesQty()) // 기존 주문의 잔량을 남은 수량으로
                        .tif(Order.Tif.IOC) // 즉시 체결 또는 취소
                        .arrivalSeq(openOrder.getArrivalSeq()) // 기존 arrivalSeq 사용
                        .createdAt(now) // 생성 시간
                        .build();

                Optional<Fill> fillOpt = match(openOrder, syntheticSellOrder, now);
                fillOpt.ifPresent(fills::add);
            }
            // 3. 매도 주문 처리: bidp1 가격으로 체결 가능한지 확인
            else if (openOrder.getSide() == Order.Side.SELL && openOrder.getPrice().compareTo(bidp1) <= 0) {
                // 가상의 매수 주문 생성 (시장가 매수 주문으로 간주)
                Order syntheticBuyOrder = Order.builder()
                        .orderId(new OrderId("SYNTHETIC-BUY-" + openOrder.getOrderId().value() + "-" + now.toEpochMilli())) // 고유 ID 생성
                        .symbol(symbol)
                        .side(Order.Side.BUY)
                        .type(Order.Type.MARKET)
                        .price(bidp1) // 체결 가격은 bidp1
                        .origQty(openOrder.getLeavesQty())
                        .leavesQty(openOrder.getLeavesQty())
                        .tif(Order.Tif.IOC)
                        .arrivalSeq(openOrder.getArrivalSeq())
                        .createdAt(now)
                        .build();

                Optional<Fill> fillOpt = match(openOrder, syntheticBuyOrder, now);
                fillOpt.ifPresent(fills::add);
            }
        }
        return fills;
    }
}
