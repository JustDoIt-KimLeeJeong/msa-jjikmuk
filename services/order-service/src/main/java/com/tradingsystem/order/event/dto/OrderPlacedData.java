package com.tradingsystem.order.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 주문 생성 이벤트 데이터
 *
 * Portfolio Service가 구독하여 자금/주식 예약 처리
 * Execution Service가 구독하여 체결 대기열에 등록
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPlacedData {

    /**
     * 주문 ID
     */
    private Long orderId;

    /**
     * 사용자 ID
     */
    private String userId;

    /**
     * 종목 코드 (6자리 숫자)
     * 예: "005930" (삼성전자)
     */
    private String symbol;

    /**
     * 매수/매도 구분
     * BUY, SELL
     */
    private String side;

    /**
     * 주문 유형
     * MARKET, LIMIT
     */
    private String orderType;

    /**
     * 주문 수량
     */
    private Integer quantity;

    /**
     * 주문 가격 (지정가만 해당)
     * 시장가는 null
     */
    private BigDecimal price;

    /**
     * 주문 생성 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 만료 시간 (지정가만 해당)
     * 시장가는 null
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiresAt;
}