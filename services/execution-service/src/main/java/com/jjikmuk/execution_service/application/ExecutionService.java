package com.jjikmuk.execution_service.application;

import com.jjikmuk.execution_service.domain.event.DomainEvent;

public interface ExecutionService {
    void onOrderAccepted(DomainEvent evt);
    void onOrderCancelled(DomainEvent evt);

}
