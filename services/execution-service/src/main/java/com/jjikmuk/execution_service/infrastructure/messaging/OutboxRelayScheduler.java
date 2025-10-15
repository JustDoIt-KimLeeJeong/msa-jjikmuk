package com.jjikmuk.execution_service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjikmuk.execution_service.domain.event.payload.TradeExecuted;
import com.jjikmuk.execution_service.infrastructure.persistence.entity.OutboxEventEntity;
import com.jjikmuk.execution_service.infrastructure.persistence.repository.OutboxJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OutboxRelayScheduler {

    private final OutboxJpaRepository outboxJpaRepository;
    private final ExecutionEventsProducer producer;
    private final ObjectMapper objectMapper;

    // 200ms 마다 100건씩 발행
    @Scheduled(fixedDelay = 200)
    @Transactional
    public void relay() {
        List<OutboxEventEntity> batch = outboxJpaRepository.findTop100ByOrderByCreatedAtAsc();

        for (OutboxEventEntity event : batch) {
            // TODO: 현재는 TradeExecuted 이벤트만 처리하고 있지만, 향후 다른 이벤트 타입도 처리할 수 있도록 확장 필요
            if ("TradeExecuted".equals(event.getEventType())) {
                try {
                    TradeExecuted payload = objectMapper.readValue(event.getPayload(), TradeExecuted.class);
                    producer.publishTradeExecuted(payload);
                    outboxJpaRepository.delete(event);
                } catch (Exception e) {
                    // TODO: 로깅 및 에러 처리 (e.g., 재시도 횟수 제한, Dead Letter Queue로 이동)
                }
            }
        }
    }
}
