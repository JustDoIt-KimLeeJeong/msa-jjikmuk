package com.jjikmuk.execution_service.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainEvent<T> {
    private String eventId;      // UUID string
    private String eventType;    // e.g. "TradeExecuted"
    private String aggregateId;  // e.g. orderId
    private Instant timestamp;   // event creation time
    private T data;              // generic

}