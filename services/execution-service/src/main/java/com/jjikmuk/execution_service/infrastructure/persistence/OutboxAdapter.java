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

    /**
     * 체결 완료(TradeExecuted) 이벤트를 Outbox에 저장합니다.
     *
     * @param dto TradeExecuted 이벤트 페이로드
     */
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

    /**
     * 주문 취소 성공(OrderCancelSucceeded) 이벤트를 Outbox에 저장합니다.
     *
     * @param dto 이벤트 페이로드 (Object 형태로 전달되며 캐스팅 수행)
     */
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

    /**
     * 주문 취소 거절(OrderCancelRejected) 이벤트를 Outbox에 저장합니다.
     *
     * @param dto 이벤트 페이로드 (Object 형태로 전달되며 캐스팅 수행)
     */
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

    /**
     * 이벤트 DTO를 JSON 문자열로 변환합니다.
     *
     * @param dto 직렬화할 객체
     * @return JSON 문자열
     * @throws RuntimeException 직렬화 실패 시 래핑된 예외 발생
     */
    private String toJson(Object dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event payload to JSON", e);
        }
    }
}
