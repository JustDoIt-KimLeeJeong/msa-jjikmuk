package com.jjikmuk.execution_service.domain.event;

import java.util.List;

public record TradeExecuted (
        String eventId, String orderId, String symbol, String side,
        List<FillLine> fills, long cumQty, long leavesQty
){
    public record FillLine(String price, long qty, String executedAt) {}
}
