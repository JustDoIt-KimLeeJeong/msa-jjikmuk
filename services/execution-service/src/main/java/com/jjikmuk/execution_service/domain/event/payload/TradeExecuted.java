package com.jjikmuk.execution_service.domain.event.payload;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 개별 체결(Fill)이 발생했음을 알리는 이벤트 페이로드
 *
 * @param orderId       주문 ID
 * @param symbol        심볼
 * @param side          매수/매도
 * @param price         이번 체결의 가격
 * @param executedQty   이번 체결의 수량
 * @param leavesQty     이번 체결 후 남은 주문 잔량
 * @param orderStatus   이번 체결 후 주문의 상태
 * @param executedAt    체결 시각
 */
public record TradeExecuted(
    String orderId,
    String symbol,
    String side,
    BigDecimal price,
    long executedQty,
    long leavesQty,
    OrderStatus orderStatus,
    Instant executedAt
) {
    public enum OrderStatus {
        PARTIALLY_FILLED, // 부분 체결
        FILLED            // 전량 체결
    }
}
