package com.jjikmuk.execution_service.domain.model;

import com.jjikmuk.execution_service.domain.model.value.OrderId;
import com.jjikmuk.execution_service.domain.model.value.Symbol;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Builder
public class Trade {
    // 주문 단위 누적 체결 요약(누적/잔량)

    private final String  tradeId;
    private final OrderId orderId;
    private final Symbol    symbol;
    private final Order.Side side;
    private final Instant createdAt;
    @Setter private long  cumQty;
    @Setter private long  leavesQty;

}
