package com.jjikmuk.execution_service.application.mapper;

import com.jjikmuk.execution_service.domain.event.payload.OrderAccepted;
import com.jjikmuk.execution_service.domain.model.Order;
import com.jjikmuk.execution_service.domain.model.value.OrderId;
import com.jjikmuk.execution_service.domain.model.value.Symbol;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

@Component
public class OrderMapper {

    /**
     * OrderAccepted 이벤트를 기반으로 도메인 Order 객체를 생성합니다.
     *
     * @param payload   주문 접수 이벤트 페이로드
     * @param arrivalSeq 해당 주문의 도착 순번 (Symbol 단위로 증가)
     * @param createdAt  주문 생성 시각
     * @return 변환된 도메인 Order 객체
     */
    public Order toDomain(OrderAccepted payload, long arrivalSeq, Instant createdAt) {
        return Order.builder()
            .orderId(new OrderId(payload.orderId()))
            .symbol(new Symbol(payload.symbol()))
            .side(payload.sideEnum())
            .type(payload.typeEnum())
            .price(payload.price() == null ? null : new BigDecimal(payload.price()))
            .origQty(payload.quantity())
            .leavesQty(payload.quantity()) // 초기 잔량은 주문 수량과 동일
            .tif(payload.tifEnum() != null ? payload.tifEnum() : Order.Tif.IOC)
            .arrivalSeq(arrivalSeq)
            .createdAt(createdAt)
            .build();
    }
}
