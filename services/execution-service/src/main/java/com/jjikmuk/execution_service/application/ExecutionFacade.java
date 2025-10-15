package com.jjikmuk.execution_service.application;

import com.jjikmuk.execution_service.application.mapper.OrderMapper;
import com.jjikmuk.execution_service.domain.event.DomainEvent;
import com.jjikmuk.execution_service.domain.event.payload.*;
import com.jjikmuk.execution_service.domain.model.Fill;
import com.jjikmuk.execution_service.domain.model.Order;
import com.jjikmuk.execution_service.domain.model.value.OrderId;
import com.jjikmuk.execution_service.domain.model.value.Symbol;
import com.jjikmuk.execution_service.domain.port.ExecutionRepository;
import com.jjikmuk.execution_service.domain.port.OutboxPort;
import com.jjikmuk.execution_service.domain.port.SymbolSeqPort;
import com.jjikmuk.execution_service.domain.service.MatchingEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExecutionFacade {

    private final ExecutionRepository executionRepository;
    private final MatchingEngine matchingEngine;
    private final OutboxPort outboxPort;
    private final TimeProvider timeProvider;
    private final OrderMapper orderMapper;
    private final SymbolSeqPort symbolSeqPort;

    @Transactional
    public void handleOrderAccepted(DomainEvent event) {
        OrderAccepted payload = (OrderAccepted) event.getData();

        Instant now = timeProvider.now();
        log.info("[OrderAccepted] eventId={} symbol={} side={} qty={}",
                event.getEventId(), payload.symbol(), payload.side(), payload.quantity());

        // 1. Mapper를 사용해 이벤트 페이로드로부터 도메인 모델(Order) 생성
        long arrivalSeq = symbolSeqPort.nextArrivalSeq(new Symbol(payload.symbol()));   // arrivalSeq 발급. - 시간 우선 보장
        Order incomingOrder = orderMapper.toDomain(payload, arrivalSeq, now);           // Order 객체 생성
        log.debug("[OrderAccepted] Mapped incomingOrder={}", incomingOrder);

        // 2. 체결 로직: 주문 잔량이 있고, 체결 조건이 맞으면 계속해서 오더북의 반대 주문과 매칭 시도
        while (incomingOrder.getLeavesQty() > 0) {
            // 2.1. 가장 유리한 상대 주문 조회 (Best Opposite Order)
            Optional<Order> bestOppositeOpt = executionRepository.peekBestOpposite(incomingOrder.getSymbol(), incomingOrder.getSide());

            // 2.2. 반대 주문이 없거나 가격이 맞지 않으면 매칭 중단
            if (bestOppositeOpt.isEmpty() || !incomingOrder.crosses(bestOppositeOpt.get().getPrice())) {
                log.info("[Matching] No opposite order found. Stop matching. orderId={}", incomingOrder.getOrderId());
                break;
            }

            Order openOrder = bestOppositeOpt.get();

            // 2.3. MatchingEngine에 매칭 위임
            Optional<Fill> fillOpt = matchingEngine.match(incomingOrder, openOrder, now);

            if (fillOpt.isPresent()) {
                Fill fill = fillOpt.get();

                // [1. ID 생성]
                // 매수-매도 주문 매칭에 대한 고유한 거래 ID(tradeId)를 생성합니다.
                // 이 ID는 양쪽 주문의 체결 이벤트에 동일하게 사용되어, 하나의 거래로 묶어줍니다.
                String tradeId = UUID.randomUUID().toString();

                // [2. DB 저장]
                // 생성된 tradeId를 포함하여 체결 내역(Fill)을 DB에 기록합니다.
                // 양쪽 주문(incoming, open) 모두에 대해 동일한 tradeId가 저장됩니다.
                executionRepository.upsertTradeAndAppendFills(tradeId, incomingOrder.getOrderId(), incomingOrder.getSide(), incomingOrder.getSymbol(), List.of(fill), fill.qty(), now);
                executionRepository.upsertTradeAndAppendFills(tradeId, openOrder.getOrderId(), openOrder.getSide(), openOrder.getSymbol(), List.of(fill), fill.qty(), now);

                // [3. 이벤트 생성]
                // Outbox 패턴에 따라 발행할 체결 이벤트를 생성합니다.

                // 3.1. 신규 주문(Taker)에 대한 이벤트
                TradeExecuted.OrderStatus incomingStatus = (incomingOrder.getLeavesQty() > 0) ? TradeExecuted.OrderStatus.PARTIALLY_FILLED : TradeExecuted.OrderStatus.FILLED;
                FillPayload incomingFillPayload = new FillPayload(fill.price(), fill.qty());
                TradeExecuted incomingTradeEvent = new TradeExecuted(
                    tradeId, // <-- 1번에서 생성한 ID 사용
                    incomingOrder.getOrderId().value(),
                    incomingOrder.getSymbol().value(),
                    incomingOrder.getSide().name(),
                    incomingFillPayload,
                    incomingOrder.getLeavesQty(),
                    incomingStatus,
                    now
                );
                outboxPort.saveTradeExecuted(incomingTradeEvent);

                // 3.2. 기존 주문(Maker)에 대한 이벤트
                TradeExecuted.OrderStatus openStatus = (openOrder.getLeavesQty() > 0) ? TradeExecuted.OrderStatus.PARTIALLY_FILLED : TradeExecuted.OrderStatus.FILLED;
                FillPayload openFillPayload = new FillPayload(fill.price(), fill.qty());
                TradeExecuted openTradeEvent = new TradeExecuted(
                    tradeId, // <-- 1번에서 생성한 동일한 ID 사용
                    openOrder.getOrderId().value(),
                    openOrder.getSymbol().value(),
                    openOrder.getSide().name(),
                    openFillPayload,
                    openOrder.getLeavesQty(),
                    openStatus,
                    now
                );
                outboxPort.saveTradeExecuted(openTradeEvent);

                // 2.6. 오더북에 있던 주문이 전량 체결되었으면 오더북에서 제거
                if (openOrder.getLeavesQty() == 0) {
                    executionRepository.removeOpen(openOrder);
                }
            } else {
                // 매칭이 더 이상 불가능하면 중단
                break;
            }
        }

        // 3. 후처리: 미체결 잔량 처리
        if (incomingOrder.getLeavesQty() > 0) {
            if (incomingOrder.isMarket()) {
                // 시장가 주문의 미체결 잔량은 즉시 취소 처리
                log.warn("시장가 주문이 전량 체결되지 못했습니다. 미체결 수량은 자동 취소됩니다. OrderId : {}", incomingOrder.getOrderId().value());
                OrderCancelSucceeded cancelSucceeded = new OrderCancelSucceeded(
                                incomingOrder.getOrderId().value(),
                                "Unfilled market order quantity cancelled",
                                now.toString());
                outboxPort.saveCancelSucceeded(cancelSucceeded);
            } else {
                // 지정가 주문의 미체결 잔량은 오더북에 등록
                executionRepository.insertOpen(incomingOrder);
            }
        }
    }

    @Transactional
    public void handleOrderCancelled(DomainEvent event) {
        OrderCancelled payload = (OrderCancelled) event.getData();
        Instant now = timeProvider.now();

        // 1. 오더북에서 해당 주문을 찾아 삭제 시도 (String을 OrderId 값 객체로 변환)
        boolean removed = executionRepository.deleteOpenIfExists(new OrderId(payload.orderId()));

        // 2. 삭제 성공/실패에 따라 적절한 이벤트 발행 (Outbox)
        if (removed) {
            OrderCancelSucceeded succeeded = new OrderCancelSucceeded(
                payload.orderId(),
                payload.reason(),
                now.toString()
            );
            outboxPort.saveCancelSucceeded(succeeded);
        } else {
            OrderCancelRejected rejected = new OrderCancelRejected(
                payload.orderId(),
                "Order not found or already filled",
                now.toString()
            );
            outboxPort.saveCancelRejected(rejected);
        }
    }

    @Transactional
    public void matchOrders(String symbolValue, BigDecimal bidp1, BigDecimal askp1) {
        Symbol symbol = new Symbol(symbolValue);
        Instant now = timeProvider.now();

        // MatchingEngine에 Tick 정보를 전달하여 오더북의 주문들과 체결 시도
        // 이 부분은 MatchingEngine에 새로운 메서드가 필요합니다.
        // matchingEngine.processTick(symbol, bidp1, askp1, now);
        log.info("Processing tick for symbol {} with bidp1={} and askp1={}", symbolValue, bidp1, askp1);
        // TODO: MatchingEngine에 processTick 메서드 구현 후 호출
    }

    @Transactional
    public void processMarketDataTick(String symbolValue, BigDecimal bidp1, BigDecimal askp1) {
        Symbol symbol = new Symbol(symbolValue);
        Instant now = timeProvider.now();

        // MatchingEngine에 Tick 정보를 전달하여 오더북의 주문들과 체결 시도
        List<Fill> fills = matchingEngine.processTick(symbol, bidp1, askp1, now);

        for (Fill fill : fills) {
            // A single tradeId for the match against the market tick
            String tradeId = UUID.randomUUID().toString();

            // Fill 처리 로직 (handleOrderAccepted에서 복사 및 Tick 기반으로 수정)
            Optional<Order> filledOpenOrderOpt = executionRepository.findOpen(fill.orderId());
            if (filledOpenOrderOpt.isEmpty()) {
                log.warn("Filled order not found in repository for OrderId: {}", fill.orderId().value());
                continue;
            }
            Order filledOpenOrder = filledOpenOrderOpt.get();

            // MatchingEngine.processTick에서 이미 leavesQty가 업데이트된 상태로 Fill이 생성됨.
            // 여기서는 Fill 정보를 바탕으로 Trade 및 이벤트 발행만 처리.

            // 가상 주문 재구성 (Trade 및 이벤트 발행을 위해)
            Order syntheticCounterOrder = Order.builder()
                    .orderId(new OrderId("SYNTHETIC-COUNTER-" + filledOpenOrder.getOrderId().value() + "-" + now.toEpochMilli()))
                    .symbol(symbol)
                    .side(filledOpenOrder.getSide() == Order.Side.BUY ? Order.Side.SELL : Order.Side.BUY) // 반대 사이드
                    .type(Order.Type.MARKET)
                    .price(filledOpenOrder.getSide() == Order.Side.BUY ? askp1 : bidp1) // 체결 가격
                    .origQty(fill.qty()) // 체결 수량만큼
                    .leavesQty(0L) // 가상 주문은 항상 전량 체결된 것으로 간주
                    .tif(Order.Tif.IOC)
                    .arrivalSeq(filledOpenOrder.getArrivalSeq())
                    .createdAt(now)
                    .build();

            // 2.4. 체결 결과(Fill)를 양쪽 주문의 거래(Trade)에 각각 반영
            executionRepository.upsertTradeAndAppendFills(tradeId, filledOpenOrder.getOrderId(), filledOpenOrder.getSide(), filledOpenOrder.getSymbol(), List.of(fill), filledOpenOrder.getLeavesQty(), now);
            executionRepository.upsertTradeAndAppendFills(tradeId, syntheticCounterOrder.getOrderId(), syntheticCounterOrder.getSide(), syntheticCounterOrder.getSymbol(), List.of(fill), syntheticCounterOrder.getLeavesQty(), now);

            // 2.5. 양쪽 주문에 대한 체결 이벤트를 각각 발행 (Outbox)

            // 2.5.1. 오더북 주문에 대한 이벤트
            TradeExecuted.OrderStatus filledOrderStatus = (filledOpenOrder.getLeavesQty() > 0) ? TradeExecuted.OrderStatus.PARTIALLY_FILLED : TradeExecuted.OrderStatus.FILLED;
            FillPayload filledFillPayload = new FillPayload(fill.price(), fill.qty());
            TradeExecuted filledOrderEvent = new TradeExecuted(
                tradeId,
                filledOpenOrder.getOrderId().value(),
                filledOpenOrder.getSymbol().value(),
                filledOpenOrder.getSide().name(),
                filledFillPayload,
                filledOpenOrder.getLeavesQty(),
                filledOrderStatus,
                now
            );
            outboxPort.saveTradeExecuted(filledOrderEvent);

            // 2.5.2. 가상 주문에 대한 이벤트 (항상 FILLED)
            FillPayload syntheticFillPayload = new FillPayload(fill.price(), fill.qty());
            TradeExecuted syntheticOrderEvent = new TradeExecuted(
                tradeId,
                syntheticCounterOrder.getOrderId().value(),
                syntheticCounterOrder.getSymbol().value(),
                syntheticCounterOrder.getSide().name(),
                syntheticFillPayload,
                0L, // 가상 주문은 잔량 0
                TradeExecuted.OrderStatus.FILLED,
                now
            );
            outboxPort.saveTradeExecuted(syntheticOrderEvent);

            // 2.6. 오더북에 있던 주문이 전량 체결되었으면 오더북에서 제거
            if (filledOpenOrder.getLeavesQty() == 0) {
                executionRepository.removeOpen(filledOpenOrder);
            }
            // 가상 주문은 오더북에 없으므로 제거하지 않음.
        }
    }
}
