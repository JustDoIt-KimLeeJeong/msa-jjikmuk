package com.jjikmuk.execution_service.infrastructure.messaging;

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

    // 200ms 마다 100건씩 발행
    @Scheduled(fixedDelay = 200)
    @Transactional
    public void relay() {
//        List<OutboxEventEntity> batch = outboxJpaRepository.findTop100ByStatusOrderByIdAsc("PENDING");
//        for (OutboxEventEntity e : batch) {
//        }
    }
}
