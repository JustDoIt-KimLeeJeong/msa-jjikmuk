package com.jjikmuk.execution_service.domain.port;

import com.jjikmuk.execution_service.domain.model.Fill;
import com.jjikmuk.execution_service.domain.model.Order;
import com.jjikmuk.execution_service.domain.model.value.OrderId;
import com.jjikmuk.execution_service.domain.model.value.Symbol;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ExecutionRepository {
    long nextArrivalSeq(Symbol symbol);

    // 시장가
    void saveTradeWithFills(Order order, List<Fill> fills, Instant now);
    boolean removeIfPending(OrderId id);
    void reserve(Order order);                          // 최근가 없을 때 예약(선택)

    // 지정가 (오더북)
    void insertOpen(Order order);
    boolean deleteOpenIfExists(OrderId orderId);
    Optional<Order> peekBestOpposite(Symbol symbol, Order.Side incomingSide);
    void removeOpen(Order order);
    Optional<Order> findOpen(OrderId id);

    // 체결/필
    void upsertTradeAndAppendFills(OrderId orderId, Order.Side side, Symbol symbol,
                                   List<Fill> fills, long totalQty, Instant now);


}
