package com.jjikmuk.execution_service.domain.service;

import com.jjikmuk.execution_service.domain.model.Fill;
import com.jjikmuk.execution_service.domain.model.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Component
public class MatchingEngine {

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
        return Optional.of(new Fill(tradePrice, tradeQty, now));
    }
}

