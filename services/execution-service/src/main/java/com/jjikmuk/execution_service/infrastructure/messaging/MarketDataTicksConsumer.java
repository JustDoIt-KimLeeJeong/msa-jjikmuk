package com.jjikmuk.execution_service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjikmuk.execution_service.application.ExecutionFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketDataTicksConsumer {

    private final ExecutionFacade executionFacade;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = "${spring.kafka.topic.market-data-ticks}",
        containerFactory = "tickKafkaListenerContainerFactory"
    )
    public void consume(String message) {
        log.info("Consumed market data tick: {}", message);
        try {
            MarketDataTick tick = objectMapper.readValue(message, MarketDataTick.class);
            // MatchingEngine에 Tick 정보를 전달하여 체결 로직 수행
            executionFacade.matchOrders(tick.symbol(), tick.bidp1(), tick.askp1());
        } catch (Exception e) {
            log.error("Failed to process market data tick: {}", message, e);
        }
    }

    // 사용자 제공 정보에 따른 MarketDataTick 구조
    private record MarketDataTick(
            String symbol,
            BigDecimal bidp1,       // 최우선 매수호가
            long bidp_rsqn1,        // L1 매수 잔량
            BigDecimal askp1,       // 최우선 매도호가
            long askp_rsqn1         // L1 매도 잔량
    ) {}
}
