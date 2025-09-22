package com.jjikmuk.execution_service.infrastructure.messaging;

import com.jjikmuk.execution_service.application.ExecutionService;
import com.jjikmuk.execution_service.domain.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventsConsumer {

    private final ExecutionService executionService;

    @KafkaListener(
            topics = "order-events",
            groupId = "execution-service",
            concurrency = "30", // 파티션 수와 맞추면 1:1 매핑. - maximum 30.
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void listen(DomainEvent evt)
//                       @Header(name = "kafka_receivePartitionId",required = false) Integer p, // 파티션 번호
//                       @Header(name = "kafka_offset", required = false) Long off)
    {
        try {
            switch (evt.getEventType()){
                case "OrderAccepted" -> executionService.onOrderAccepted(evt);
                case "OrderCancelled" -> executionService.onOrderCancelled(evt);
                default -> log.warn("UnKnown eventType: {}", evt.getEventType());
            }
        } catch (Exception e) {
            log.error("OrderEventsConsumer error evtId={}, type={}",
//                    evt.getEventId(), evt.getEventType(), p, off, e);
                    evt.getEventId(), evt.getEventType(), e);
            throw e; // 재시도/DLT는 이후 설정에서
        }


    }
}
