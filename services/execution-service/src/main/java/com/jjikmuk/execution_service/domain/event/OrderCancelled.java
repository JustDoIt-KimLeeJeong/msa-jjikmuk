package com.jjikmuk.execution_service.domain.event;

public record OrderCancelled (
    String orderId, String reason
){
}
