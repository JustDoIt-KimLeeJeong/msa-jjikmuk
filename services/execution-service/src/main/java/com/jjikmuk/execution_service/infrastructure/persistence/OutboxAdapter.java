package com.jjikmuk.execution_service.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjikmuk.execution_service.domain.event.payload.OrderCancelRejected;
import com.jjikmuk.execution_service.domain.event.payload.OrderCancelSucceeded;
import com.jjikmuk.execution_service.domain.event.payload.TradeExecuted;
import com.jjikmuk.execution_service.domain.port.OutboxPort;
import com.jjikmuk.execution_service.infrastructure.persistence.entity.OutboxEventEntity;
import com.jjikmuk.execution_service.infrastructure.persistence.repository.OutboxJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxAdapter implements OutboxPort {

    private static final String AGGREGATE_TYPE = "ORDER";

    private final OutboxJpaRepository outboxJpaRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void saveTradeExecuted(TradeExecuted dto) {
        TradeExecuted payload = dto;
        String jsonPayload = toJson(payload);
        OutboxEventEntity entity = new OutboxEventEntity(
            AGGREGATE_TYPE,
            payload.orderId(),
            TradeExecuted.class.getSimpleName(), // 패키지 경로를 제외한 순수한 클래스 이름을 문자열로 가져옴. "TradeExecuted" 도 무관.
            jsonPayload
        );
        outboxJpaRepository.save(entity);
    }

    @Override
    public void saveCancelSucceeded(Object dto) {
        OrderCancelSucceeded payload = (OrderCancelSucceeded) dto;
        String jsonPayload = toJson(payload);
        OutboxEventEntity entity = new OutboxEventEntity(
            AGGREGATE_TYPE,
            payload.orderId(),
            OrderCancelSucceeded.class.getSimpleName(),
            jsonPayload
        );
        outboxJpaRepository.save(entity);
    }

    @Override
    public void saveCancelRejected(Object dto) {
        OrderCancelRejected payload = (OrderCancelRejected) dto;
        String jsonPayload = toJson(payload);
        OutboxEventEntity entity = new OutboxEventEntity(
            AGGREGATE_TYPE,
            payload.orderId(),
            OrderCancelRejected.class.getSimpleName(),
            jsonPayload
        );
        outboxJpaRepository.save(entity);
    }

    private String toJson(Object dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event payload to JSON", e);
        }
    }
}
