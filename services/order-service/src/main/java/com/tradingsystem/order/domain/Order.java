package com.tradingsystem.order.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 주문 엔티티
 * - 주문의 메타데이터 및 상태 관리
 * - 시장가/지정가 구분
 * - 만료 시간 관리 (LIMIT만 설정)
 */

@Entity
@Table(name = "orders",
        indexes = {
            @Index(name = "idx_user_id_created_at", columnList = "userId, createdAt DESC")
        },
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_user_client_order", columnNames = {"userId", "clientOrderId"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@EntityListeners(AuditingEntityListener.class)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    /**
     * 클라이언트 주문 ID (멱등성 보장용)
     */
    @Column(name = "client_order_id", nullable = false, length = 100)
    private String clientOrderId;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OrderSide side;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OrderType type;

    @Column(precision = 19, scale = 4)
    private BigDecimal price; // 지정가 주문(LIMIT)일 경우 필수 입력

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    @Builder.Default
    private Integer filledQuantity = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    private LocalDateTime expiresAt; // 지정가 주문 만료 시간

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // === 비즈니스 메서드 ===

    /**
     * 주문 상태 업데이트
     */
    public void updateStatus(OrderStatus newStatus) {
        this.status = newStatus;
    }

    /**
     * 체결 수량 추가 및 상태 업데이트
     * @param quantity 실제로 체결된 수량
     */
    public void addFilledQuantity(Integer quantity) {
        if (this.filledQuantity + quantity > this.quantity) {
            throw new IllegalArgumentException("체결 수량(" + quantity + ")이 남은 수량(" + getRemainingQuantity() + ")을 초과합니다.");
        }

        this.filledQuantity += quantity;

        if (this.filledQuantity.equals(this.quantity)) {
            this.status = OrderStatus.FILLED;
        } else if (this.filledQuantity > 0) {
            this.status = OrderStatus.PARTIALLY_FILLED;
        }
    }

    /**
     * 시장가 주문 여부
     */
    public boolean isMarketOrder() {
        return this.type == OrderType.MARKET;
    }

    /**
     * 지정가 주문 여부
     */
    public boolean isLimitOrder() {
        return this.type == OrderType.LIMIT;
    }

    /**
     * 매수 주문 여부
     */
    public boolean isBuyOrder() {
        return this.side == OrderSide.BUY;
    }

    /**
     * 매도 주문 여부
     */
    public boolean isSellOrder() {
        return this.side == OrderSide.SELL;
    }

    /**
     * 남은 수량 계산
     */
    public Integer getRemainingQuantity() {
        return this.quantity - this.filledQuantity;
    }

    /**
     * 주문 만료 여부 확인
     * @param checkTime  만료 여부를 확인하려는 기준 시간
     */
    public boolean isExpired(LocalDateTime checkTime) {
        // 시장가 주문은 만료 시간이 없습니다.
        if (this.type == OrderType.MARKET) {
            return false;
        }

        // 만료 시간이 설정되어 있고, 기준 시간이 만료 시간을 지났다면 true
        return expiresAt != null && checkTime.isAfter(expiresAt);
    }

    /**
     * 주문이 현재 체결 가능 상태(Active)인지 확인
     * (PENDING 또는 PARTIALLY_FILLED 상태이며, 만료되지 않음)
     * @param checkTime 체결 가능 여부를 확인하려는 기준 시간
     */
    public boolean isTradable(LocalDateTime checkTime) {
        // 1. 상태가 체결 가능한 상태인지 확인
        boolean isActiveStatus = this.status == OrderStatus.PENDING || this.status == OrderStatus.PARTIALLY_FILLED;

        if (!isActiveStatus) {
            return false; // 이미 FILLED, CANCELED, EXPIRED 상태 등
        }

        // 2. 지정가 주문인 경우 만료 여부 확인
        if (this.isLimitOrder() && this.isExpired(checkTime)) {
            return false;
        }

        return true;
    }


}

