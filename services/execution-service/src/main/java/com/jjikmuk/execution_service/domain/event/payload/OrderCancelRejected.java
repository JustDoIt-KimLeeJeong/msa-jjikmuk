package com.jjikmuk.execution_service.domain.event.payload;

public record OrderCancelRejected (String orderId, String reason, String decidedAt) {}

