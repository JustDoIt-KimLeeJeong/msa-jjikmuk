package com.jjikmuk.execution_service.application;

import com.jjikmuk.execution_service.application.mapper.OrderMapper;
import com.jjikmuk.execution_service.domain.event.DomainEvent;
import com.jjikmuk.execution_service.domain.event.payload.OrderAccepted;
import com.jjikmuk.execution_service.domain.event.payload.OrderCancelled;
import com.jjikmuk.execution_service.domain.event.payload.OrderCancelRejected;
import com.jjikmuk.execution_service.domain.event.payload.OrderCancelSucceeded;
import com.jjikmuk.execution_service.domain.event.payload.TradeExecuted;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;

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

        // 1. Mapper를 사용해 이벤트 페이로드로부터 도메인 모델(Order) 생성
        long arrivalSeq = symbolSeqPort.nextArrivalSeq(new Symbol(payload.symbol()));   // arrivalSeq 발급. - 시간 우선 보장
        Order incomingOrder = orderMapper.toDomain(payload, arrivalSeq, now);           // Order 객체 생성

        // TODO: 멱등성 체크 (이미 처리된 eventId인지 확인)

        // 2. 체결 로직: 주문 잔량이 있고, 체결 조건이 맞으면 계속해서 오더북의 반대 주문과 매칭 시도
        while (incomingOrder.getLeavesQty() > 0) {
            // 2.1. 가장 유리한 상대 주문 조회 (Best Opposite Order)
            Optional<Order> bestOppositeOpt = executionRepository.peekBestOpposite(incomingOrder.getSymbol(), incomingOrder.getSide());

            // 2.2. 반대 주문이 없거나 가격이 맞지 않으면 매칭 중단
            if (bestOppositeOpt.isEmpty() || !incomingOrder.crosses(bestOppositeOpt.get().getPrice())) {
                break;
            }

            Order openOrder = bestOppositeOpt.get();

            // 2.3. MatchingEngine에 매칭 위임
            Optional<Fill> fillOpt = matchingEngine.match(incomingOrder, openOrder, now);

            if (fillOpt.isPresent()) {
                Fill fill = fillOpt.get();

                // 2.4. 체결 결과(Fill)를 양쪽 주문의 거래(Trade)에 각각 반영
                executionRepository.upsertTradeAndAppendFills(incomingOrder.getOrderId(), incomingOrder.getSide(), incomingOrder.getSymbol(), List.of(fill), fill.qty(), now);
                executionRepository.upsertTradeAndAppendFills(openOrder.getOrderId(), openOrder.getSide(), openOrder.getSymbol(), List.of(fill), fill.qty(), now);

                // 2.5. 양쪽 주문에 대한 체결 이벤트를 각각 발행 (Outbox)

                // 2.5.1. 신규 주문(Taker)에 대한 이벤트
                TradeExecuted.OrderStatus incomingStatus = (incomingOrder.getLeavesQty() > 0) ? TradeExecuted.OrderStatus.PARTIALLY_FILLED : TradeExecuted.OrderStatus.FILLED;
                TradeExecuted incomingTradeEvent = new TradeExecuted(
                    incomingOrder.getOrderId().value(),
                    incomingOrder.getSymbol().value(),
                    incomingOrder.getSide().name(),
                    fill.price(),
                    fill.qty(),
                    incomingOrder.getLeavesQty(),
                    incomingStatus,
                    now
                );
                outboxPort.saveTradeExecuted(incomingTradeEvent);

                // 2.5.2. 기존 주문(Maker)에 대한 이벤트
                TradeExecuted.OrderStatus openStatus = (openOrder.getLeavesQty() > 0) ? TradeExecuted.OrderStatus.PARTIALLY_FILLED : TradeExecuted.OrderStatus.FILLED;
                TradeExecuted openTradeEvent = new TradeExecuted(
                    openOrder.getOrderId().value(),
                    openOrder.getSymbol().value(),
                    openOrder.getSide().name(),
                    fill.price(),
                    fill.qty(),
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
}
