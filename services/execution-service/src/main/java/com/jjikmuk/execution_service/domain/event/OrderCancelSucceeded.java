package com.jjikmuk.execution_service.domain.event;

public record OrderCancelSucceeded(String orderId, String reason, String decidedAt) {}

