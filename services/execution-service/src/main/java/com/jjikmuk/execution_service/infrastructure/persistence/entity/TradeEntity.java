package com.jjikmuk.execution_service.infrastructure.persistence.entity;

import com.jjikmuk.execution_service.domain.model.Order;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trade")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TradeEntity {

    @Id
    @Column(length = 36)
    private String tradeId; // orderId와 동일한 값을 사용

    @Column(length = 36, unique = true, nullable = false)
    private String orderId;

    @Column(length = 20, nullable = false)
    private String symbol;

    @Column(length = 4, nullable = false)
    @Enumerated(EnumType.STRING)
    private Order.Side side;

    @Column(nullable = false)
    private long cumQty; // 누적 체결 수량

    @Column(nullable = false)
    private long leavesQty; // 남은 수량

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    @OneToMany(mappedBy = "trade", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FillEntity> fills = new ArrayList<>();

    public TradeEntity(String orderId, String symbol, Order.Side side, long leavesQty) {
        this.tradeId = orderId;
        this.orderId = orderId;
        this.symbol = symbol;
        this.side = side;
        this.cumQty = 0L;
        this.leavesQty = leavesQty;
    }

    public void addFill(FillEntity fill) {
        this.fills.add(fill);
        this.cumQty += fill.getQuantity();
    }

    public void setLeavesQty(long leavesQty) {
        this.leavesQty = leavesQty;
    }
}
