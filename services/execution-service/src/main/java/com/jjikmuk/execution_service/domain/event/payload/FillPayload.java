package com.jjikmuk.execution_service.domain.event.payload;

import java.math.BigDecimal;

/**
 * TradeExecuted 이벤트에 포함되는 개별 체결 정보를 담는 객체
 * @param price 체결 가격
 * @param qty 체결 수량
 */
public record FillPayload(
    BigDecimal price,
    long qty
) {}