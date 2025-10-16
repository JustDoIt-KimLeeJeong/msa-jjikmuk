package com.jjikmuk.execution_service.domain.event.payload;

import java.time.Instant;

/**
 * 개별 체결(Fill) 발생과 그로 인한 주문 상태 변경을 알리는 이벤트 페이로드
 *
 * @param tradeId     거래 ID
 * @param orderId     주문 ID
 * @param symbol      심볼
 * @param side        매수/매도
 * @param fill        이번 체결에 대한 상세 정보 (가격, 수량)
 * @param leavesQty   체결 후 주문의 남은 잔량
 * @param orderStatus 체결 후 주문의 상태
 * @param executedAt  체결 시각
 */
public record TradeExecuted(
    String tradeId,
    String orderId,
    String symbol,
    String side,
    FillPayload fill,
    long leavesQty,
    OrderStatus orderStatus,
    Instant executedAt
) {
    public enum OrderStatus {
        PARTIALLY_FILLED, // 부분 체결
        FILLED            // 전량 체결
    }
}