package com.jjikmuk.execution_service.domain.port;

public interface OutboxPort {
    void saveTradeExecuted(Object dto);      // TradeExecuted 등
    void saveCancelSucceeded(Object dto);
    void saveCancelRejected(Object dto);
}
