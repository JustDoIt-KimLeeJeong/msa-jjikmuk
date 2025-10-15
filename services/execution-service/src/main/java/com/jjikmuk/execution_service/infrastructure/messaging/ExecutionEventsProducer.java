package com.jjikmuk.execution_service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjikmuk.execution_service.domain.event.DomainEvent;
import com.jjikmuk.execution_service.domain.event.payload.TradeExecuted;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExecutionEventsProducer {

    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;
    @Value("${spring.kafka.topic.execution-events}")
    private String TOPIC;

        /**
         * TradeExecuted 이벤트를 Kafka로 발행합니다.
         * 이 메소드는 OutboxRelayScheduler에 의해 호출됩니다.
         */
        public void publishTradeExecuted(TradeExecuted payload) {
            // 최종적으로 Kafka에 발행될 DomainEvent 객체를 생성합니다.
            DomainEvent evt = DomainEvent.builder()
                    .eventId(UUID.randomUUID().toString())  // 이벤트 자체의 고유 ID 생성
                    .eventType("TradeExecuted")             // 이벤트 타입
                    .aggregateId(payload.orderId())         // 이 이벤트가 속한 주문(Aggregate)의 ID를 지정
                    .timestamp(Instant.now())

                    .data(objectMapper.valueToTree(payload))    // payload를 JsonNode 트리 형태로 변환하여 data 필드에 담습니다.
                    .build();
    
            // Kafka 토픽으로 이벤트를 전송합니다.
            // 메시지 키로 aggregateId(orderId)를 사용하여, 동일한 주문에 대한 이벤트들이
            // 항상 같은 파티션에 순서대로 저장되도록 보장합니다.
            kafkaTemplate.send(TOPIC, evt.getAggregateId(), evt);
        }
}
