package com.tradingsystem.order.service;

import com.tradingsystem.order.domain.*;
import com.tradingsystem.order.dto.request.CreateOrderRequest;
import com.tradingsystem.order.dto.response.OrderResponse;
import com.tradingsystem.order.dto.response.PageResponse;
import com.tradingsystem.order.event.EventPublisher;
import com.tradingsystem.order.event.dto.OrderCancelledData;
import com.tradingsystem.order.event.dto.OrderPlacedData;
import com.tradingsystem.order.exception.DuplicateOrderException;
import com.tradingsystem.order.exception.InvalidOrderStatusException;
import com.tradingsystem.order.exception.OrderNotFoundException;
import com.tradingsystem.order.exception.UnauthorizedOrderAccessException;
import com.tradingsystem.order.repository.OrderRepository;
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

/**
 * 주문 서비스
 * - 주문 생성/취소/조회 비즈니스 로직 처리
 * - EventPublisher를 통한 Outbox 패턴 이벤트 발행
 * - 시장가/지정가 구분 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;

    private static final int LIMIT_ORDER_EXPIRY_HOURS = 24;

    /**
     * 주문 생성
     *
     * 플로우:
     * 1. clientOrderId 중복 체크
     * 2. Order 엔티티 생성 및 저장
     * 3. OrderPlaced 이벤트 발행 (EventPublisher)
     * 4. 트랜잭션 커밋 → CDC가 Kafka로 자동 발행
     *
     * @param userId BFF에서 JWT 토큰으로부터 추출한 사용자 ID
     * @param request 주문 생성 요청
     * @return 생성된 주문 정보
     * @throws DuplicateOrderException clientOrderId 중복 시
     */
    @Transactional
    public OrderResponse createOrder(String userId, CreateOrderRequest request) {
        log.info("주문 생성 시작: userId={}, clientOrderId={}, symbol={}, side={}, type={}",
                userId, request.getClientOrderId(), request.getSymbol(),
                request.getSide(), request.getType());

        try {
            // 1. 주문 유형별 가격 검증
            request.validateOrderTypeAndPrice();

            // 2. 중복 주문 체크
            validateDuplicateOrder(userId, request.getClientOrderId());

            // 3. Order 엔티티 생성
            Order order = buildOrder(userId, request);

            // 4. DB 저장
            Order savedOrder = orderRepository.save(order);
            log.debug("주문 저장 완료: orderId={}", savedOrder.getId());

            // 5. 이벤트 데이터 생성
            OrderPlacedData eventData = buildOrderPlacedData(savedOrder);

            // 6. Outbox에 이벤트 발행 (같은 트랜잭션)
            // EventPublisher가 MDC에서 correlationId 자동 추출
            eventPublisher.publish("order.placed", savedOrder.getId(), eventData);

            log.info("주문 생성 완료: orderId={}, status={}",
                    savedOrder.getId(), savedOrder.getStatus());

            return OrderResponse.from(savedOrder);

        } catch (DuplicateOrderException e) {
            log.warn("중복 주문 시도: userId={}, clientOrderId={}",
                    userId, request.getClientOrderId());
            throw e;
        } catch (IllegalArgumentException e) {
            log.warn("주문 검증 실패: userId={}, error={}", userId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("주문 생성 실패: userId={}, symbol={}",
                    userId, request.getSymbol(), e);
            throw new RuntimeException("주문 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 주문 취소
     *
     * 플로우:
     * 1. 주문 존재 및 소유권 확인
     * 2. 취소 가능 상태 확인 (PENDING, ACCEPTED만)
     * 3. 주문 상태를 CANCELLED로 변경
     * 4. OrderCancelled 이벤트 발행
     * 5. Portfolio가 예약 자금/주식 해제
     *
     * @param orderId 취소할 주문 ID
     * @return 취소된 주문 정보
     * @throws OrderNotFoundException 주문을 찾을 수 없을 때
     * @throws UnauthorizedOrderAccessException 주문 소유자가 아닐 때
     * @throws InvalidOrderStatusException 취소 불가능한 상태일 때
     */
    @Transactional
    public OrderResponse cancelOrder(String userId, Long orderId) {
        try {
            // 1. 주문 조회 및 소유권 검증
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

            // 본인 주문만 취소 가능
            if (!order.getUserId().equals(userId)) {
                throw new UnauthorizedOrderAccessException(
                        "본인 주문만 취소할 수 있습니다.: " + orderId);
            }

            // 2. 취소 가능 상태 검증
            if (!order.isCancellable()) {
                throw new InvalidOrderStatusException(
                        String.format("취소할 수 없는 주문 상태입니다. orderId=%d, status=%s",
                                orderId, order.getStatus())
                );
            }

            // 3. 주문 취소 처리
            order.cancel();

            log.debug("주문이 취소되었습니다.: orderId={}, status=CANCELLED", orderId);

            // 4. 이벤트 데이터 생성
            OrderCancelledData eventData = buildOrderCancelledData(order);

            // 5. Outbox에 이벤트 발행
            eventPublisher.publish("order.cancelled", order.getId(), eventData);

            return OrderResponse.from(order);

        } catch (OrderNotFoundException | UnauthorizedOrderAccessException |
                 InvalidOrderStatusException e) {
            log.warn("주문 취소 실패: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("주문 취소 중 예상치 못한 오류: orderId={}", orderId, e);
            throw new RuntimeException("주문 취소 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 주문 단건 조회
     *
     * @param userId 사용자 ID (소유권 확인용)
     * @param orderId 조회할 주문 ID
     * @return 주문 상세 정보
     * @throws OrderNotFoundException 주문을 찾을 수 없을 때
     * @throws UnauthorizedOrderAccessException 주문 소유자가 아닐 때
     */
    public OrderResponse getOrder(String userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("주문이 존재하지 않습니다: " + orderId));

        if (!order.getUserId().equals(userId)) {
            throw new UnauthorizedOrderAccessException(
                    "본인 주문만 조회 가능합니다: " + orderId);
        }

        return OrderResponse.from(order);
    }

    /**
     * 주문 목록 조회 (페이징)
     *
     * @param userId 사용자 ID
     * @param pageable
     * @return 페이징된 주문 목록
     */
    public Page<OrderResponse> getOrders(String userId, Pageable pageable) {
        Page<Order> orders = orderRepository.findByUserId(userId, pageable);
        return orders.map(OrderResponse::from);
    }

    // ========== Private Helper Methods ==========

    /**
     * 중복 주문 검증
     */
    private void validateDuplicateOrder(String userId, String clientOrderId) {
        if (orderRepository.existsByUserIdAndClientOrderId(userId, clientOrderId)) {
            throw new DuplicateOrderException(
                    String.format("이미 존재하는 주문입니다. clientOrderId=%s", clientOrderId)
            );
        }
    }

    /**
     * Order 엔티티 생성
     */
    private Order buildOrder(String userId, CreateOrderRequest request) {
        OrderType type = request.getType();

        // 지정가는 price 필수
        BigDecimal price = null;
        if (type == OrderType.LIMIT) {
            price = BigDecimal.valueOf(request.getPrice());
        }

        // 지정가 주문만 만료 시간 설정 (24시간)
        LocalDateTime expiresAt = null;
        if (type == OrderType.LIMIT) {
            expiresAt = LocalDateTime.now().plusHours(LIMIT_ORDER_EXPIRY_HOURS);
        }

        return Order.builder()
                .userId(userId)
                .clientOrderId(request.getClientOrderId())
                .symbol(request.getSymbol())
                .side(request.getSide())
                .type(type)
                .price(price)
                .quantity(request.getQuantity())
                .filledQuantity(0)
                .status(OrderStatus.PENDING)
                .expiresAt(expiresAt)
                .build();
    }

    /**
     * OrderPlacedData 이벤트 데이터 생성
     */
    private OrderPlacedData buildOrderPlacedData(Order order) {
        return OrderPlacedData.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .symbol(order.getSymbol())
                .side(order.getSide().name())
                .orderType(order.getType().name())
                .quantity(order.getQuantity())
                .price(order.getPrice())
                .createdAt(order.getCreatedAt())
                .expiresAt(order.getExpiresAt())
                .build();
    }

    /**
     * OrderCancelledData 이벤트 데이터 생성
     */
    private OrderCancelledData buildOrderCancelledData(Order order) {
        return OrderCancelledData.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .symbol(order.getSymbol())
                .side(order.getSide().name())
                .orderType(order.getType().name())
                .quantity(order.getQuantity())
                .filledQuantity(order.getFilledQuantity())
                .cancelReason("USER_REQUESTED")
                .cancelledAt(LocalDateTime.now())
                .build();
    }
    
}