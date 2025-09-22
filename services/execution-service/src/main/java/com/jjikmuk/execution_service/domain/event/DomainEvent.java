package com.jjikmuk.execution_service.domain.event;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class DomainEvent<T> {
    private String eventId;      // UUID string
    private String eventType;    // e.g. "TradeExecuted"
//    private int version;         // schema version
    private String aggregateId;  // e.g. orderId
    private Instant timestamp;   // event creation time
    private T data;              // generic

}
/**
 * {
 *   "eventId": "UUID",              // 이벤트 고유 ID (멱등성 보장)
 *   "eventType": "TradeExecuted",   // 이벤트 타입명
 *   "version": 1,                   // 이벤트 스키마 버전
 *   "aggregateId": "ord-123",       // 관련 주문 ID
 *   "timestamp": "2025-09-16T01:00:05Z", // 이벤트 발생 시각 (UTC)
 *   "data": {   }
 *     "executedAt": "2025-09-16T01:00:05Z"
 *   }
 * }
 */