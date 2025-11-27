package com.tradingsystem.order.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_trades",
        indexes = {
            @Index(name = "idx_order_id", columnList = "orderId"),
            @Index(name = "idx_trade_id", columnList = "tradeId", unique = true)
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class OrderTrade extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false, length = 100)
    private String tradeId; // Execution에서 받은 체결 ID (멱등성 보장)

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal executedPrice;

    @Column(nullable = false)
    private Integer executedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OrderSide side;

    // === 비즈니스 메서드 ===

    /**
     * 체결 금액 계산
     */
    public BigDecimal getTotalAmount() {
        return executedPrice.multiply(BigDecimal.valueOf(executedQuantity));
    }

}
