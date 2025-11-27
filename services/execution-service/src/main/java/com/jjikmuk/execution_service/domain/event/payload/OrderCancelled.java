package com.jjikmuk.execution_service.domain.event.payload;

public record OrderCancelled (
    String orderId, String reason
){
}
