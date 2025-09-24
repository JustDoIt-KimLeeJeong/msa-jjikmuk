package com.jjikmuk.execution_service.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "fill")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FillEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fillId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_id")
    private TradeEntity trade;

    @Column(precision = 18, scale = 4, nullable = false) // scale 소수점 이하 자릿수.
    private BigDecimal price;
    
    @Column(nullable = false)
    private long quantity;

    @Column(nullable = false)
    private Instant executedAt;

    public FillEntity(TradeEntity trade, BigDecimal price, long quantity, Instant executedAt) {
        this.trade = trade;
        this.price = price;
        this.quantity = quantity;
        this.executedAt = executedAt;
    }
}
