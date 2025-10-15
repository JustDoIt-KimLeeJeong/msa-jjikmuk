package com.jjikmuk.execution_service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjikmuk.execution_service.domain.event.payload.OrderCancelRejected;
import com.jjikmuk.execution_service.domain.event.payload.OrderCancelSucceeded;
import com.jjikmuk.execution_service.domain.event.payload.TradeExecuted;
import com.jjikmuk.execution_service.infrastructure.persistence.entity.OutboxEventEntity;
import com.jjikmuk.execution_service.infrastructure.persistence.repository.OutboxJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelayScheduler {

    private final OutboxJpaRepository outboxJpaRepository;
    private final ExecutionEventsProducer producer;
    private final ObjectMapper objectMapper;

    // 200ms 마다 최대 100건씩 이벤트 발행
    @Scheduled(fixedDelay = 200)
    @Transactional
    public void relay() {
        List<OutboxEventEntity> batch = outboxJpaRepository.findTop100ByOrderByCreatedAtAsc();
        if (!batch.isEmpty()) {
            log.debug("OutboxRelayScheduler: Outbox 테이블에서 {}건의 이벤트를 조회했습니다.", batch.size());
        }

        for (OutboxEventEntity event : batch) {
            try {
                switch (event.getEventType()) {
                    case "TradeExecuted" -> {
                        TradeExecuted payload = objectMapper.readValue(event.getPayload(), TradeExecuted.class);
                        log.debug("OutboxRelayScheduler: Kafka로 TradeExecuted 이벤트를 발행합니다. payload={}", payload);
                        producer.publishTradeExecuted(payload);
                    }
                    case "OrderCancelSucceeded" -> {
                        OrderCancelSucceeded payload = objectMapper.readValue(event.getPayload(), OrderCancelSucceeded.class);
                        log.debug("OutboxRelayScheduler: Kafka로 OrderCancelSucceeded 이벤트를 발행합니다. payload={}", payload);
                        producer.publishOrderCancelSucceeded(payload);
                    }
                    case "OrderCancelRejected" -> {
                        OrderCancelRejected payload = objectMapper.readValue(event.getPayload(), OrderCancelRejected.class);
                        log.debug("OutboxRelayScheduler: Kafka로 OrderCancelRejected 이벤트를 발행합니다. payload={}", payload);
                        producer.publishOrderCancelRejected(payload);
                    }
                    default -> log.warn("OutboxRelayScheduler: 알 수 없는 이벤트 타입: {}", event.getEventType());
                }
                outboxJpaRepository.delete(event);
                log.debug("OutboxRelayScheduler: Outbox에서 이벤트(id={})를 삭제했습니다.", event.getId());
            } catch (Exception e) {
                log.error("OutboxRelayScheduler: 이벤트(id={}) 처리 중 오류 발생: {}", event.getId(), e.getMessage(), e);
                // TODO: 재시도 정책 또는 Dead Letter Queue로 이동 처리 필요
            }
        }
    }
}