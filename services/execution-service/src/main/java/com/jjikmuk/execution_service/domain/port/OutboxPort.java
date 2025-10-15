package com.jjikmuk.execution_service.domain.port;

import com.jjikmuk.execution_service.domain.event.payload.TradeExecuted;

public interface OutboxPort {
    void saveTradeExecuted(TradeExecuted dto);      // TradeExecuted 등
    void saveCancelSucceeded(Object dto);
    void saveCancelRejected(Object dto);
}
