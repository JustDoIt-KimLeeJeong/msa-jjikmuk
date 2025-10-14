package com.tradingsystem.order.service;

import com.tradingsystem.order.domain.*;
import com.tradingsystem.order.dto.request.CreateOrderRequest;
import com.tradingsystem.order.dto.response.OrderResponse;
import com.tradingsystem.order.dto.response.PageResponse;
import com.tradingsystem.order.exception.DuplicateOrderException;
import com.tradingsystem.order.exception.OrderNotFoundException;
import com.tradingsystem.order.repository.OrderRepository;
import com.tradingsystem.order.repository.OutboxEventRepository;
import com.tradingsystem.order.util.CorrelationIdValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 주문 서비스
 * - 주문 생성/취소/조회 비즈니스 로직 처리
 * - Outbox 패턴을 통한 이벤트 발행 (CDC)
 * - 시장가/지정가 구분 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    private static final int LIMIT_ORDER_EXPIRE_HOURS = 24; // 지정가 주문 만료 시간

    /**
     * 주문 생성
     * - 중복 주문 검증 (clientOrderId 기반)
     * - 시장가/지정가 구분 처리
     * - Order + OutboxEvent 동시 저장 (트랜잭션)
     *
     * @param userId 사용자 ID
     * @param request 주문 생성 요청
     * @return 생성된 주문 정보
     * @throws DuplicateOrderException 중복 주문 시
     */
    @Transactional
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        log.info("주문 생성 시작 - userId: {}, clientOrderId: {}, symbol: {}",
                userId, request.getClientOrderId(), request.getSymbol());

        // 1. correlationId 검증
        CorrelationIdValidator.validate(request.getCorrelationId());

        // 2. 주문 유형별 가격 검증 추가
        request.validateOrderTypeAndPrice();

        // 3. 중복 주문 체크
        validateDuplicateOrder(userId, request.getClientOrderId());

        // 4. 주문 생성
        Order order = buildOrder(userId, request);
        Order savedOrder = orderRepository.save(order);

        log.info("주문 생성 완료 - orderId: {}, status: {}", savedOrder.getId(), savedOrder.getStatus());

        // 5. Outbox 이벤트 생성 (같은 트랜잭션)
        createOutboxEvent(savedOrder, "OrderPlaced", request.getCorrelationId());

        return OrderResponse.from(savedOrder, request.getCorrelationId());
    }

    /**
     * 주문 취소
     * - 주문 소유권 검증
     * - PENDING/ACCEPTED 상태만 취소 가능
     * - OrderCancelled 이벤트 발행
     *
     * @param userId 사용자 ID
     * @param orderId 취소할 주문 ID
     * @return 취소된 주문 정보
     * @throws OrderNotFoundException 주문을 찾을 수 없을 때
     */
    @Transactional
    public OrderResponse cancelOrder(Long userId, Long orderId) {
        log.info("주문 취소 시작 - userId: {}, orderId: {}", userId, orderId);

        // 1. 주문 조회 및 소유권 검증
        Order order = findOrderByIdAndUserId(orderId, userId);

        // 2. 취소 가능 상태 검증
        validateCancellable(order);

        // 3. 상태 변경
        order.updateStatus(OrderStatus.CANCELLED);

        log.info("주문 취소 완료 - orderId: {}, status: {}", order.getId(), order.getStatus());

        // 4. Outbox 이벤트 생성
        String correlationId = generateCorrelationId("CANCEL");
        createOutboxEvent(order, "OrderCancelled", correlationId);

        return OrderResponse.from(order, correlationId);
    }

    /**
     * 주문 단건 조회
     * - 주문 소유권 검증
     *
     * @param userId 사용자 ID
     * @param orderId 조회할 주문 ID
     * @return 주문 상세 정보
     * @throws OrderNotFoundException 주문을 찾을 수 없을 때
     */
    public OrderResponse getOrder(Long userId, Long orderId, String correlationId) {
        log.debug("주문 단건 조회 - userId: {}, orderId: {}", userId, orderId);

        Order order = findOrderByIdAndUserId(orderId, userId);
        return OrderResponse.from(order, correlationId);
    }

    /**
     * 주문 목록 조회 (페이징)
     * - 사용자의 주문만 조회
     * - 상태별 필터링 (선택)
     * - 최신순 정렬
     *
     * @param userId 사용자 ID
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param status 주문 상태 필터 (선택)
     * @return 페이징된 주문 목록
     */
    public PageResponse<OrderResponse> getOrders(Long userId, int page, int size, String status, String correlationId) {
        log.debug("주문 목록 조회 - userId: {}, page: {}, size: {}, status: {}",
                userId, page, size, status);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Order> orderPage;
        if (status != null && !status.isBlank()) {
            OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
            orderPage = orderRepository.findByUserIdAndStatus(userId, orderStatus, pageable);
        } else {
            orderPage = orderRepository.findByUserId(userId, pageable);
        }

        Page<OrderResponse> responsePage = orderPage.map(order -> OrderResponse.from(order, correlationId));
        return PageResponse.from(responsePage);
    }

    // ===== Private Helper Methods =====

    /**
     * 중복 주문 검증
     */
    private void validateDuplicateOrder(Long userId, String clientOrderId) {
        if (orderRepository.existsByUserIdAndClientOrderId(userId, clientOrderId)) {
            log.warn("중복 주문 감지 - userId: {}, clientOrderId: {}", userId, clientOrderId);
            throw new DuplicateOrderException(
                    String.format("이미 존재하는 주문입니다. clientOrderId: %s", clientOrderId)
            );
        }
    }

    /**
     * 주문 엔티티 생성
     */
    private Order buildOrder(Long userId, CreateOrderRequest request) {
        OrderType type = request.getType();
        OrderSide side = request.getSide();

        // 시장가 주문은 price null, 지정가는 필수
        BigDecimal price = null;
        if (type == OrderType.LIMIT) {
            if (request.getPrice() == null) {
                throw new IllegalArgumentException("지정가 주문은 price가 필수입니다.");
            }
            price = BigDecimal.valueOf(request.getPrice());
        }

        // 지정가 주문만 만료 시간 설정 (24시간)
        LocalDateTime expiresAt = null;
        if (type == OrderType.LIMIT) {
            expiresAt = LocalDateTime.now().plusHours(LIMIT_ORDER_EXPIRE_HOURS);
        }

        return Order.builder()
                .userId(userId)
                .clientOrderId(request.getClientOrderId())
                .symbol(request.getSymbol())
                .side(side)
                .type(type)
                .price(price)
                .quantity(request.getQuantity())
                .filledQuantity(0)
                .status(OrderStatus.PENDING)
                .expiresAt(expiresAt)
                .build();
    }

    /**
     * Outbox 이벤트 생성
     * - Order와 같은 트랜잭션에서 저장
     * - CDC가 자동으로 Kafka에 발행
     */
    private void createOutboxEvent(Order order, String eventType, String correlationId) {
        try {
            String payload = objectMapper.writeValueAsString(
                    OrderEventPayload.builder()
                            .orderId(order.getId())
                            .userId(order.getUserId())
                            .clientOrderId(order.getClientOrderId())
                            .symbol(order.getSymbol())
                            .side(order.getSide().name())
                            .type(order.getType().name())
                            .price(order.getPrice())
                            .quantity(order.getQuantity())
                            .status(order.getStatus().name())
                            .correlationId(correlationId)
                            .build()
            );

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType(eventType)
                    .aggregateId(order.getId())
                    .payload(payload)
                    .correlationId(correlationId)
                    .published(false)
                    .build();

            outboxEventRepository.save(outboxEvent);

            log.info("Outbox 이벤트 생성 - eventType: {}, orderId: {}, eventId: {}",
                    eventType, order.getId(), outboxEvent.getEventId());

        } catch (JsonProcessingException e) {
            log.error("Outbox 이벤트 직렬화 실패 - orderId: {}", order.getId(), e);
            throw new RuntimeException("이벤트 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 주문 조회 및 소유권 검증
     */
    private Order findOrderByIdAndUserId(Long orderId, Long userId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> {
                    log.warn("주문을 찾을 수 없음 - orderId: {}, userId: {}", orderId, userId);
                    return new OrderNotFoundException(
                            String.format("주문을 찾을 수 없습니다. orderId: %d", orderId)
                    );
                });
    }

    /**
     * 취소 가능 상태 검증
     */
    private void validateCancellable(Order order) {
        if (order.getStatus() != OrderStatus.PENDING &&
                order.getStatus() != OrderStatus.ACCEPTED) {
            throw new IllegalStateException(
                    String.format("취소할 수 없는 주문 상태입니다. status: %s", order.getStatus())
            );
        }
    }

    /**
     * correlationId 생성 (취소/만료 등 내부 이벤트용)
     */
    private String generateCorrelationId(String prefix) {
        return String.format("%s_%s", prefix, UUID.randomUUID().toString());
    }

    /**
     * 이벤트 페이로드 DTO
     */
    @lombok.Builder
    @lombok.Getter
    private static class OrderEventPayload {
        private Long orderId;
        private Long userId;
        private String clientOrderId;
        private String symbol;
        private String side;
        private String type;
        private BigDecimal price;
        private Integer quantity;
        private String status;
        private String correlationId;
    }
}