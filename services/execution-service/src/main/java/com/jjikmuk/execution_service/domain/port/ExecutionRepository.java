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

    // -- 오더북 관련

    /**
     * 신규 주문을 오더북에 삽입한다.
     * - 체결되지 않은 주문(잔량)이 유지되는 영역.
     */
    void insertOpen(Order order);

    /**
     * 지정한 주문이 오더북에 존재하면 삭제한다.
     * @return true 삭제 성공, false 이미 없었음
     */
    boolean deleteOpenIfExists(OrderId orderId);

    /**
     * 반대 사이드(Buy vs Sell)에서 가장 우선순위가 높은 주문을 조회한다.
     * - Best Bid / Best Ask 를 의미.
     */
    Optional<Order> peekBestOpposite(Symbol symbol, Order.Side incomingSide);

    /**
     * 오더북에서 해당 주문을 제거한다.
     */
    void removeOpen(Order order);

    /**
     * 특정 주문을 조회한다.
     */
    Optional<Order> findOpen(OrderId id);

    /**
     * 특정 심볼에 대한 모든 오픈 주문을 조회한다.
     */
    List<Order> findAllOpenOrdersBySymbol(Symbol symbol);

    // -------------------- 체결 / Fill 관련 --------------------

    /**
     * 거래(Trade) 정보를 upsert 하고 Fill 내역을 누적한다.
     *
     * @param orderId   체결된 주문 ID
     * @param side      매수/매도
     * @param symbol    거래 심볼
     * @param fills     체결된 조각 내역
     * @param leavesQty  남은 수량
     * @param now       체결 시각
     *
     * 비고:
     * - Trade 테이블이 없으면 Insert, 있으면 Update
     * - Fill 은 체결 내역 로그로 append
     */
    void upsertTradeAndAppendFills(OrderId orderId,
                                   Order.Side side,
                                   Symbol symbol,
                                   List<Fill> fills,
                                   long leavesQty,
                                   Instant now);


}
