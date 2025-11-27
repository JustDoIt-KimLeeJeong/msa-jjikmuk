package com.jjikmuk.execution_service.infrastructure.persistence.mapper;

import com.jjikmuk.execution_service.domain.model.Fill;
import com.jjikmuk.execution_service.domain.model.Order;
import com.jjikmuk.execution_service.domain.model.value.OrderId;
import com.jjikmuk.execution_service.domain.model.value.Symbol;
import com.jjikmuk.execution_service.infrastructure.persistence.entity.FillEntity;
import com.jjikmuk.execution_service.infrastructure.persistence.entity.OrderOpenEntity;
import com.jjikmuk.execution_service.infrastructure.persistence.entity.TradeEntity;
import org.springframework.stereotype.Component;

@Component
public class PersistenceMapper {

    public OrderOpenEntity toOpenOrderEntity(Order domain) {
        return new OrderOpenEntity(
            domain.getOrderId().value(),
            domain.getSymbol().value(),
            domain.getSide(),
            domain.getType(),
            domain.getPrice(),
            domain.getLeavesQty(),
            domain.getArrivalSeq(),
            domain.getCreatedAt()
        );
    }

    public Order toDomain(OrderOpenEntity entity) {
        return Order.builder()
            .orderId(new OrderId(entity.getOrderId()))
            .symbol(new Symbol(entity.getSymbol()))
            .side(entity.getSide())
            .type(entity.getType())
            .price(entity.getPrice())
            .origQty(entity.getLeavesQty()) // Note: origQty is not stored, using leavesQty as best effort
            .leavesQty(entity.getLeavesQty())
            .tif(Order.Tif.GFD) // Note: TIF is not stored, using default
            .arrivalSeq(entity.getArrivalSeq())
            .createdAt(entity.getCreatedAt())
            .build();
    }

    public FillEntity toFillEntity(Fill fill, TradeEntity trade, String tradeId) {
        return new FillEntity(
            trade,
            tradeId,
            fill.price(),
            fill.qty(),
            fill.executedAt()
        );
    }
}
