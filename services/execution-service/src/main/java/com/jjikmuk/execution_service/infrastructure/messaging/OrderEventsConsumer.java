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

    @KafkaListener(
        topics = "${spring.kafka.topic.order-events}",
        groupId = "execution-service",
        containerFactory = "domainEventKafkaListenerContainerFactory"
    )
    public void listen(DomainEvent evt)
    {
        try {
            switch (evt.getEventType()) {
                case "OrderAccepted" -> {
                    OrderAccepted payload = objectMapper.convertValue(evt.getData(), OrderAccepted.class);
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
            log.error("OrderEventsConsumer error evtId={}, type={}",
//                    evt.getEventId(), evt.getEventType(), p, off, e);
                    evt.getEventId(), evt.getEventType(), e);
            throw e; // 재시도/DLT는 이후 설정에서
        }


    }
}
