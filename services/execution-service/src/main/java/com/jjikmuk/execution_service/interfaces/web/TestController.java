package com.jjikmuk.execution_service.interfaces.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjikmuk.execution_service.domain.event.DomainEvent;
import com.jjikmuk.execution_service.domain.event.payload.OrderAccepted;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;


import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * 개발 및 테스트용 컨트롤러.
 */

@RestController
@RequestMapping("/testcon")
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final KafkaTemplate<String, DomainEvent> domainEventKafkaTemplate;
    private final KafkaTemplate<String, String> stringKafkaTemplate;
    private final ObjectMapper objectMapper;

    @GetMapping("/get")
    public String testtest() {
        return "gggg";
    }
    /**
     * OrderAccepted 이벤트를 Kafka 'order-events' 토픽으로 발행합니다.
     */
    @PostMapping("/publish/order-accepted")
    public String publishOrderAccepted(@RequestBody OrderAcceptedRequest request) {
        OrderAccepted payload = new OrderAccepted(
            request.getEventId(),
            request.getOrderId(),
            request.getUserId(),
            request.getSymbol(),
            request.getSide(),
            request.getType(),
            request.getTif(),
            request.getPrice(),
            request.getQuantity(),
            request.getIsMarketHours()
        );

        DomainEvent<OrderAccepted> domainEvent = DomainEvent.<OrderAccepted>builder()
            .eventId(request.getEventId() != null ? request.getEventId() : UUID.randomUUID().toString())
            .eventType("OrderAccepted")
            .aggregateId(request.getOrderId())
            .timestamp(Instant.now())
            .data(payload)
            .build();

        log.debug("DomainEvent 생성 중: {}", domainEvent);
        domainEventKafkaTemplate.send("order-events", domainEvent.getAggregateId(), domainEvent);
        log.debug("Kafka에 OrderAccepted 이벤트 발행 완료: {}", domainEvent.getEventId());
        return "Published OrderAccepted event to Kafka: " + domainEvent.getEventId();
    }

    /**
     * MarketDataTick 이벤트를 Kafka 'market-data-ticks' 토픽으로 발행합니다.
     */
    @SneakyThrows
    @PostMapping("/publish/market-tick")
    public String publishMarketTick(@RequestBody MarketDataTickRequest request) {
        String payload = objectMapper.writeValueAsString(request);
        stringKafkaTemplate.send("market-data-ticks", payload);
        return "Published MarketDataTick event to Kafka: " + payload;
    }

    // --- Request DTOs ---

    @Data
    static class OrderAcceptedRequest {
        private String eventId;
        private String orderId;
        private String userId;
        private String symbol;
        private String side;
        private String type;
        private String tif = "GFD";
        private String price;
        private Long quantity;
        private Boolean isMarketHours = true;
    }

    @Data
    static class MarketDataTickRequest {
        private String symbol;
        private BigDecimal bidp1;
        private long bidp_rsqn1;
        private BigDecimal askp1;
        private long askp_rsqn1;
    }

    @Data
    static class PlaceOrderRequest {
        /**
         * 주문 ID. 제공되지 않으면 UUID가 자동으로 생성됩니다.
         */
        private String orderId;
        /**
         * 사용자 ID. 제공되지 않으면 테스트용 사용자 ID가 자동으로 생성됩니다.
         */
        private String userId;
        /**
         * 거래 심볼 (예: "BTC/USD").
         */
        private String symbol;
        /**
         * 주문 유형 (MARKET 또는 LIMIT).
         */
        private String type; // MARKET or LIMIT
        /**
         * 주문 유효 기간 (GFD 또는 IOC). 기본값은 GFD.
         */
        private String tif = "GFD"; // GFD or IOC
        /**
         * 지정가 주문의 경우 필수. 시장가 주문의 경우 무시됩니다.
         */
        private String price; // Required for LIMIT orders
        /**
         * 주문 수량.
         */
        private Long quantity;
    }

    /**
     * 매수 주문을 생성하고 Kafka 'order-events' 토픽으로 발행합니다.
     * 내부적으로 publishOrderAccepted 메서드를 호출합니다.
     * @param request 주문 요청 정보 (symbol, type, price, quantity 등)
     * @return Kafka 발행 결과 메시지
     */
    @PostMapping("/place-buy-order")
    public String placeBuyOrder(@RequestBody PlaceOrderRequest request) {
        OrderAcceptedRequest orderAcceptedRequest = new OrderAcceptedRequest();
        orderAcceptedRequest.setOrderId(request.getOrderId() != null ? request.getOrderId() : UUID.randomUUID().toString());
        orderAcceptedRequest.setUserId(request.getUserId() != null ? request.getUserId() : "test-user-" + UUID.randomUUID().toString().substring(0, 8));
        orderAcceptedRequest.setSymbol(request.getSymbol());
        orderAcceptedRequest.setSide("BUY");
        orderAcceptedRequest.setType(request.getType());
        orderAcceptedRequest.setTif(request.getTif());
        orderAcceptedRequest.setPrice(request.getPrice());
        orderAcceptedRequest.setQuantity(request.getQuantity());
        orderAcceptedRequest.setIsMarketHours(true);

        return publishOrderAccepted(orderAcceptedRequest);
    }

    /**
     * 매도 주문을 생성하고 Kafka 'order-events' 토픽으로 발행합니다.
     * 내부적으로 publishOrderAccepted 메서드를 호출합니다.
     * @param request 주문 요청 정보 (symbol, type, price, quantity 등)
     * @return Kafka 발행 결과 메시지
     */
    @PostMapping("/place-sell-order")
    public String placeSellOrder(@RequestBody PlaceOrderRequest request) {
        log.debug("placeSellOrder 요청 수신: {}", request);
        OrderAcceptedRequest orderAcceptedRequest = new OrderAcceptedRequest();
        orderAcceptedRequest.setOrderId(request.getOrderId() != null ? request.getOrderId() : UUID.randomUUID().toString());
        orderAcceptedRequest.setUserId(request.getUserId() != null ? request.getUserId() : "test-user-" + UUID.randomUUID().toString().substring(0, 8));
        orderAcceptedRequest.setSymbol(request.getSymbol());
        orderAcceptedRequest.setSide("SELL");
        orderAcceptedRequest.setType(request.getType());
        orderAcceptedRequest.setTif(request.getTif());
        orderAcceptedRequest.setPrice(request.getPrice());
        orderAcceptedRequest.setQuantity(request.getQuantity());
        orderAcceptedRequest.setIsMarketHours(true);
        log.debug("Kafka 발행을 위한 OrderAcceptedRequest 준비: {}", orderAcceptedRequest);
        return publishOrderAccepted(orderAcceptedRequest);
    }
}