package com.jjikmuk.execution_service.application;

import com.jjikmuk.execution_service.domain.event.DomainEvent;
import com.jjikmuk.execution_service.domain.port.LastPricePort;
import com.jjikmuk.execution_service.domain.port.OutboxPort;
import com.jjikmuk.execution_service.domain.port.SymbolSeqPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExecutionFacade {

    private final OutboxPort outboxPort;
    private final LastPricePort lastPricePort;
    private final SymbolSeqPort symbolSeqPort;

    public void handleOrderAccepted(DomainEvent evt) {
        // 1. 멱등 체크
        // 2. 즉시 체결 or 예약 저장
        // 3. Outbox 이벤트 저장
    }

    public void handleOrderCancelled(DomainEvent evt) {
        // 1. 예약 상태면 삭제 → CancelSucceeded 이벤트 Outbox 저장
        // 2. 이미 트리거면 CancelRejected 이벤트 Outbox 저장
    }
}
