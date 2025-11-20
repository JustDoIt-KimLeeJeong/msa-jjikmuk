package com.tradingsystem.order.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 주문 취소 이벤트 데이터
 *
 * Portfolio Service가 구독하여 예약된 자금/주식 해제
 * Execution Service가 구독하여 오더북에서 제거
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelledData {

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
     * 주문 유형
     */
    private String orderType;

    /**
     * 원래 주문 수량
     */
    private Integer quantity;

    /**
     * 이미 체결된 수량
     * 부분 체결 후 취소의 경우 0이 아님
     */
    private Integer filledQuantity;

    /**
     * 취소 사유
     * USER_REQUESTED, SYSTEM_ERROR 등
     */
    private String cancelReason;

    /**
     * 취소 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime cancelledAt;
}