package com.tradingsystem.order.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 모든 도메인 이벤트의 공통 래퍼
 * Kafka로 발행되는 표준 메세지 형식
 */

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainEvent<T> {

    /**
     * 이벤트 고유 ID (UUID)
     */
    private String eventId;

    /**
     * 이벤트 타입 (PascalCase)
     * 예: "OrderPlaced", "OrderCancelled"
     */
    private String eventType;

    /**
     * 분산 추적 ID
     * BFF에서 생성되어 전체 플로우에서 동일하게 유지
     */
    private String correlationId;

    /**
     * 이벤트 발생 시간 (ISO 8601 형식)
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * 이벤트 발생 서비스
     */
    private String source;

    /**
     * 이벤트 상세 데이터
     * 타입별로 다른 DTO 사용
     */
    private T data;
}