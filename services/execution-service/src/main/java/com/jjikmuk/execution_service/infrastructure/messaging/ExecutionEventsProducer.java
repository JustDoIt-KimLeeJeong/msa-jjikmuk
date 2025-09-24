package com.jjikmuk.execution_service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjikmuk.execution_service.domain.event.DomainEvent;
import com.jjikmuk.execution_service.domain.event.payload.TradeExecuted;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExecutionEventsProducer {

    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOPIC = "execution-events"; //TODO: 환경변수로 바꿔라

    public void publishTradeExecuted(TradeExecuted payload) {
        DomainEvent evt = DomainEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("TradeExecuted")
                .aggregateId(payload.orderId())
                .timestamp(Instant.now())
                .data(objectMapper.valueToTree(payload)) // data -> JsonNode
                .build();

        // key = aggregatedId : 같은 주문은 같은 파티션 -> 순서 보장.
        kafkaTemplate.send(TOPIC, evt.getAggregateId(), evt);
    }

}
