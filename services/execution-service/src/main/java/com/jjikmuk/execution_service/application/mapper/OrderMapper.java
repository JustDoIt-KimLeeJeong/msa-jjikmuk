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
