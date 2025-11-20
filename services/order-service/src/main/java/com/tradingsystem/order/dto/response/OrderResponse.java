package com.tradingsystem.order.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tradingsystem.order.domain.Order;
import com.tradingsystem.order.domain.OrderSide;
import com.tradingsystem.order.domain.OrderStatus;
import com.tradingsystem.order.domain.OrderType;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 주문 응답 DTO
 * - Entity를 프론트엔드 친화적 형태로 변환
 * - 필요한 정보만 노출
 * - 민감 정보 제외
 * - 한국 주식 : 가격을 Integer로 반환
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@ToString
public class OrderResponse {

    /**
     * 주문 ID
     */
    private Long orderId;

    /**
     * 분산 추적 ID (Correlation ID)
     * - 요청 추적용 (디버깅/모니터링)
     */
    private String correlationId;

    /**
     * 클라이언트 주문 ID (멱등성 확인용)
     */
    private String clientOrderId;

    /**
     * 종목 코드
     */
    private String symbol;

    /**
     * 매수/매도 구분
     */
    private OrderSide side;

    /**
     * 주문 유형 (시장가/지정가)
     */
    private OrderType type;

    /**
     * 지정가 (지정가 주문만 존재, 한국 주식은 정수)
     */
    private Integer price;

    /**
     * 주문 수량
     */
    private Integer quantity;

    /**
     * 체결 수량 (누적)
     */
    private Integer filledQuantity;

    /**
     * 남은 수량
     */
    private Integer remainingQuantity;

    /**
     * 주문 상태
     */
    private OrderStatus status;

    /**
     * 주문 상태 표시명 (프론트엔드용)
     */
    private String statusDisplayName;

    /**
     * 만료 시간(지정가 주문만 존재)
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt;

    /**
     * 주문 생성 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 주문 수정 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // === 정적 팩토리 메서드 ===

    /**
     * Entity → DTO 변환
     * - BigDecimal → Integer 변환 (한국 주식 정수 가격)
     *
     * @param order 주문 엔티티
     * @param correlationId 분산 추적 ID (서비스 레이어에서 주입)
     * @return 주문 응답 DTO
     */
    public static OrderResponse from(Order order, String correlationId) {
        return OrderResponse.builder()
                .orderId(order.getId())
                .correlationId(correlationId)
                .clientOrderId(order.getClientOrderId())
                .symbol(order.getSymbol())
                .side(order.getSide())
                .type(order.getType())
                .price(order.getPrice() != null ? order.getPrice().intValue() : null)
                .quantity(order.getQuantity())
                .filledQuantity(order.getFilledQuantity())
                .remainingQuantity(order.getRemainingQuantity())
                .status(order.getStatus())
                .statusDisplayName(order.getStatus().getDisplayName())
                .expiresAt(order.getExpiresAt())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    // 오버로딩 메서드 추가
    public static OrderResponse from(Order order) {
        String correlationId = org.slf4j.MDC.get("correlationId");
        return from(order, correlationId);
    }


    // === 편의 메서드 ===

    /**
     * 체결률 계산 (백분율)
     */
    public double getFilledPercentage() {
        if (quantity == 0) {
            return 0.0;
        }
        return (filledQuantity * 100.0) / quantity;
    }

    /**
     * 완전 체결 여부
     */
    public boolean isFullyFilled() {
        return status == OrderStatus.FILLED;
    }

    /**
     * 부분 체결 여부
     */
    public boolean isPartiallyFilled() {
        return status == OrderStatus.PARTIALLY_FILLED;
    }

    /**
     * 활성 상태 여부 (체결 가능 상태)
     */
    public boolean isActive() {
        return status == OrderStatus.PENDING
                || status == OrderStatus.ACCEPTED
                || status == OrderStatus.PARTIALLY_FILLED;
    }
}
