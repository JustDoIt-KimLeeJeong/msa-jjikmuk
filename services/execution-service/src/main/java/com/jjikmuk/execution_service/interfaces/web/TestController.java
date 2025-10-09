package com.jjikmuk.execution_service.interfaces.web;

import com.jjikmuk.execution_service.application.ExecutionFacade;
import com.jjikmuk.execution_service.domain.event.DomainEvent;
import com.jjikmuk.execution_service.domain.event.payload.OrderAccepted;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/test")
public class TestController {

    private final ExecutionFacade executionFacade;

    public TestController(ExecutionFacade executionFacade) {
        this.executionFacade = executionFacade;
    }

    @PostMapping("/order-accepted")
    public String placeOrderAccepted(@RequestBody OrderAccepted orderAccepted) {
        // Create a DomainEvent wrapper for the OrderAccepted payload
        DomainEvent<OrderAccepted> domainEvent = DomainEvent.<OrderAccepted>builder()
                .eventId(UUID.randomUUID().toString()) // Unique ID for this DomainEvent instance
                .eventType("OrderAccepted")
                .aggregateId(orderAccepted.orderId()) // The orderId is the aggregateId for this event
                .timestamp(Instant.now())
                .data(orderAccepted)
                .build();

        executionFacade.handleOrderAccepted(domainEvent);
        return "OrderAccepted event processed successfully.";
    }
}
