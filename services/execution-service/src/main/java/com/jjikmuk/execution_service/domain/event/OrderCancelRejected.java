package com.jjikmuk.execution_service.domain.event;

public record OrderCancelRejected (String orderId, String reason, String decidedAt) {}

