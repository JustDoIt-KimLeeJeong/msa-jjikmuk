package com.jjikmuk.execution_service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjikmuk.execution_service.application.ExecutionService;
import com.jjikmuk.execution_service.domain.event.DomainEvent;
import com.jjikmuk.execution_service.domain.event.payload.OrderAccepted;
import com.jjikmuk.execution_service.domain.event.payload.OrderCancelled;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventsConsumer {

    private final ExecutionService executionService;
    private final ObjectMapper objectMapper;

    /**
     * 주문 이벤트 수신 리스너.
     *
     * containerFactory = "domainEventKafkaListenerContainerFactory" 에서
     * 역직렬화, 에러 핸들링, ack 모드 등을 구성합니다.
     *
     * @param evt 수신된 도메인 이벤트 래퍼 (헤더/메타 포함)
     */
    @KafkaListener(
        topics = "${spring.kafka.topic.order-events}",
        groupId = "execution-service",
        containerFactory = "domainEventKafkaListenerContainerFactory"
    )
    public void listen(DomainEvent evt)
    {
        log.debug("DomainEvent 수신: {}", evt);
        try {
            // 이벤트 타입별로 페이로드 클래스를 결정하여 안전하게 변환한 뒤, 도메인 서비스로 위임
            switch (evt.getEventType()) {
                case "OrderAccepted" -> {
                    // 1) 제네릭 data → 구체 타입으로 매핑
                    OrderAccepted payload = objectMapper.convertValue(evt.getData(), OrderAccepted.class);
                    log.debug("OrderAccepted 페이로드 처리 중: {}", payload);

                    // 2) 도메인 서비스로 래핑 이벤트 전달
                    executionService.onOrderAccepted(new DomainEvent<>(
                            evt.getEventId(),
                            evt.getEventType(),
                            evt.getAggregateId() != null ? evt.getAggregateId() : payload.eventId()+"aggId",
                            evt.getTimestamp(),
                            payload
                    ));
                }
                case "OrderCancelled" -> {
                    OrderCancelled payload = objectMapper.convertValue(evt.getData(), OrderCancelled.class);
                    log.debug("OrderCancelled 페이로드 처리 중: {}", payload);
                    executionService.onOrderCancelled(new DomainEvent<>(
                            evt.getEventId(),
                            evt.getEventType(),
                            evt.getAggregateId(),
                            evt.getTimestamp(),
                            payload
                    ));
                }
                default -> log.warn("⚠️ Unknown eventType: {}", evt.getEventType());
            }
        } catch (Exception e) {
            // 핸들러 레벨 예외 — 컨테이너 설정에 따라 재시도/Backoff/DLT 이동
            log.error("OrderEventsConsumer error evtId={}, type={}",
                    evt.getEventId(), evt.getEventType(), e
            );
            throw e; // 재시도/DLT는 이후 설정에서
        }


    }
}
