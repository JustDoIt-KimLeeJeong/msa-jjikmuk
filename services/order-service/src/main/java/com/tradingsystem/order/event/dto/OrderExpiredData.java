package com.tradingsystem.order.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 주문 만료 이벤트 데이터
 *
 * Portfolio Service가 구독하여 예약된 자금/주식 해제
 * Execution Service가 구독하여 오더북에서 제거
 *
 * 지정가 주문이 24시간 내 체결되지 않아 자동 만료된 경우
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderExpiredData {

    /**
     * 주문 ID
     */
    private Long orderId;

    /**
     * 사용자 ID
     */
    private String userId;

    /**
     * 종목 코드
     */
    private String symbol;

    /**
     * 매수/매도 구분
     */
    private String side;

    /**
     * 주문 유형 (항상 LIMIT)
     */
    private String orderType;

    /**
     * 주문 수량
     */
    private Integer quantity;

    /**
     * 이미 체결된 수량
     */
    private Integer filledQuantity;

    /**
     * 지정가
     */
    private BigDecimal price;

    /**
     * 원래 만료 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiresAt;

    /**
     * 실제 만료 처리 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiredAt;
}