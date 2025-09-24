package com.jjikmuk.execution_service.domain.event.payload;

public record OrderCancelSucceeded(String orderId, String reason, String decidedAt) {}

