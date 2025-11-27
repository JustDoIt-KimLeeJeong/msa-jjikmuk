package com.tradingsystem.order.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingsystem.order.domain.OutboxEvent;
import com.tradingsystem.order.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 도메인 이벤트를 Outbox 테이블에 저장하는 헬퍼 클래스
 * CDC가 자동으로 Kafka로 발행
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private static final String SERVICE_NAME = "order-service";
    private static final String CORRELATION_ID_KEY = "correlationId";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * 도메인 이벤트를 Outbox에 발행
     *
     * @param eventType 이벤트 타입 (예: "order.placed")
     * @param aggregateId 주문 ID (이벤트의 주체)
     * @param eventData 이벤트 데이터 객체
     */
    @Transactional
    public void publish(String eventType, Long aggregateId, Object eventData) {
        try {
            // 1. CorrelationId MDC에서 추출 (없으면 새로 생성)
            String correlationId = extractCorrelationId();

            // 2. EventId 자동 생성
            String eventId = UUID.randomUUID().toString();

            // 3. DomainEvent 래핑
            DomainEvent<?> domainEvent = DomainEvent.builder()
                    .eventId(eventId)
                    .eventType(convertToEventTypeName(eventType))
                    .correlationId(correlationId)
                    .timestamp(LocalDateTime.now())
                    .source(SERVICE_NAME)
                    .data(eventData)
                    .build();

            // 4. JSON 직렬화
            String payload = objectMapper.writeValueAsString(domainEvent);

            // 5. Outbox 이벤트 생성
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventId(eventId)
                    .correlationId(correlationId)
                    .eventType(eventType)  // CDC Router가 사용할 타입
                    .aggregateId(aggregateId)
                    .payload(payload)
                    .published(false)  // CDC가 발행하면 나중에 true로 변경 가능
                    .build();

            // 6. DB 저장 (트랜잭션 커밋 시 CDC가 감지)
            outboxEventRepository.save(outboxEvent);

            log.info("Event published to outbox: eventType={}, eventId={}, correlationId={}, aggregateId={}",
                    eventType, eventId, correlationId, aggregateId);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event data: eventType={}, aggregateId={}",
                    eventType, aggregateId, e);
            throw new RuntimeException("Event serialization failed", e);
        } catch (Exception e) {
            log.error("Failed to publish event: eventType={}, aggregateId={}",
                    eventType, aggregateId, e);
            throw new RuntimeException("Event publishing failed", e);
        }
    }

    /**
     * MDC에서 CorrelationId 추출
     * BFF에서 전달받아야 하므로 없으면 예외 발생
     *
     * @return correlationId
     */
    private String extractCorrelationId() {
        String correlationId = MDC.get(CORRELATION_ID_KEY);

        return correlationId;
    }

    /**
     * 이벤트 타입을 PascalCase로 변환
     * "order.placed" → "OrderPlaced"
     */
    private String convertToEventTypeName(String eventType) {
        String[] parts = eventType.split("\\.");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            result.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1));
        }
        return result.toString();
    }

}
