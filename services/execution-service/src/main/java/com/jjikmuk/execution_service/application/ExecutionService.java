package com.jjikmuk.execution_service.application;

import com.jjikmuk.execution_service.domain.event.DomainEvent;

/**
 * ExecutionService
 *
 * 주문 실행(Execution) 관련 도메인 이벤트를 처리하기 위한 애플리케이션 서비스 인터페이스.
 * 외부 어댑터(예: Kafka Consumer)에서 이벤트를 수신하면,
 * 이 인터페이스의 구현체를 통해 비즈니스 로직이 수행된다.
 */
public interface ExecutionService {
    /**
     * 주문 접수(OrderAccepted) 이벤트를 처리한다.
     * 신규 주문을 생성하고, 체결 가능 여부를 판단하여
     * 매칭 및 체결 이벤트를 Outbox에 적재한다.
     */
    void onOrderAccepted(DomainEvent evt);

    /**
     * 주문 취소(OrderCancelled) 이벤트를 처리한다.
     * 오더북에서 해당 주문을 제거하고, 성공/실패 결과를
     * Outbox 이벤트로 적재한다.
     */
    void onOrderCancelled(DomainEvent evt);

}
