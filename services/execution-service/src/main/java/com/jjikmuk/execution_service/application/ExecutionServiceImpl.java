package com.jjikmuk.execution_service.application;

import com.jjikmuk.execution_service.domain.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * ExecutionServiceImpl
 *
 * 주문 실행 관련 이벤트를 처리하는 서비스 구현체.
 * 외부 어댑터(Kafka Consumer 등)에서 호출되어,
 * 실제 비즈니스 로직을 담당하는 ExecutionFacade로 위임한다.
 */
@Service
@RequiredArgsConstructor
public class ExecutionServiceImpl implements ExecutionService {

    private final ExecutionFacade facade;

    /**
     * 주문 접수(OrderAccepted) 이벤트 처리 진입점.
     * 단순히 ExecutionFacade로 위임하여
     * 도메인 레벨의 체결 및 Outbox 로직을 수행한다.
     */
    @Override
    public void onOrderAccepted(DomainEvent evt) {
        facade.handleOrderAccepted(evt);

    }

    /**
     * 주문 취소(OrderCancelled) 이벤트 처리 진입점.
     * ExecutionFacade로 위임하여
     * 오더북 삭제 및 취소 이벤트 발행을 수행한다.
     */
    @Override
    public void onOrderCancelled(DomainEvent evt) {
        facade.handleOrderCancelled(evt);

    }
}
