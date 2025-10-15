package com.jjikmuk.execution_service.infrastructure.persistence.entity;

import com.jjikmuk.execution_service.domain.model.Order;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "order_open", indexes = {
    @Index(name = "idx_order_open_lookup", columnList = "symbol, side, price, arrivalSeq")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderOpenEntity {

    @Id
    @Column(length = 36)
    private String orderId;

    @Column(length = 20, nullable = false)
    private String symbol;

    @Column(length = 4, nullable = false)
    @Enumerated(EnumType.STRING)
    private Order.Side side;

    @Column(length = 10, nullable = false)
    @Enumerated(EnumType.STRING)
    private Order.Type type;

    @Column(precision = 18, scale = 4)
    private BigDecimal price;

    @Column(nullable = false)
    @Setter
    private long leavesQty;

    @Column(nullable = false)
    private long arrivalSeq;

    @Column(nullable = false)
    private Instant createdAt;

    public OrderOpenEntity(String orderId, String symbol, Order.Side side, Order.Type type, BigDecimal price, long leavesQty, long arrivalSeq, Instant createdAt) {
        this.orderId = orderId;
        this.symbol = symbol;
        this.side = side;
        this.type = type;
        this.price = price;
        this.leavesQty = leavesQty;
        this.arrivalSeq = arrivalSeq;
        this.createdAt = createdAt;
    }
}
