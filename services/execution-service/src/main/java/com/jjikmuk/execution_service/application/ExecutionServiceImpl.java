package com.jjikmuk.execution_service.application;

import com.jjikmuk.execution_service.domain.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExecutionServiceImpl implements ExecutionService {

    private final ExecutionFacade facade;

    @Override
    public void onOrderAccepted(DomainEvent evt) {
        facade.handleOrderAccepted(evt);

    }

    @Override
    public void onOrderCancelled(DomainEvent evt) {
        facade.handleOrderCancelled(evt);

    }
}
