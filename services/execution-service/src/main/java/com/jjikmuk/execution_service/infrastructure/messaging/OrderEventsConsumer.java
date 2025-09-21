package com.jjikmuk.execution_service.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderEventsConsumer {

//    @KafkaListener(topics = "")
    public void consume(){
        //
    }
}
