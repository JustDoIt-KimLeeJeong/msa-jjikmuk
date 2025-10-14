package com.tradingsystem.order.controller;

import com.tradingsystem.order.dto.request.CreateOrderRequest;
import com.tradingsystem.order.dto.response.OrderResponse;
import com.tradingsystem.order.dto.response.PageResponse;
import com.tradingsystem.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 주문 관리 컨트롤러
 * - 주문 생성/취소/조회 엔드포인트 제공
 * - BFF를 통해 JWT 인증된 요청만 처리
 * - correlationId를 통한 분산 추적 지원
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 주문 생성
     *
     * @param userId BFF가 JWT에서 추출하여 헤더로 전달한 사용자 ID
     * @param correlationId 분산 추적 ID (BFF에서 생성한 UUID v7)
     * @param request 주문 생성 요청 DTO
     * @return 생성된 주문 정보
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Correlation-Id") String correlationId,
            @Valid @RequestBody CreateOrderRequest request
            ) {
        log.info("주문 생성 요청 - correlationId: {}, userId: {}, symbol: {}, side: {}, type: {}",
                correlationId, userId, request.getSymbol(), request.getSide(), request.getType());

        OrderResponse response = orderService.createOrder(userId, request, correlationId);

        log.info("주문 생성 완료 - orderId: {}, correlationId: {}", response.getOrderId(), correlationId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 주문 취소
     *
     * @param userId BFF가 JWT에서 추출하여 헤더로 전달한 사용자 ID
     * @param correlationId 분산 추적 ID
     * @param orderId 취소할 주문 ID
     * @return 취소된 주문 정보
     */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<OrderResponse> cancelOrder(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Correlation-Id") String correlationId,
            @PathVariable Long orderId
    ) {
        log.info("주문 취소 요청 - correlationId: {}, userId: {}, orderId: {}",
                correlationId, userId, orderId);

        OrderResponse response = orderService.cancelOrder(userId, orderId, correlationId);

        log.info("주문 취소 완료 - orderId: {}, correlationId: {}", orderId, correlationId);
        return ResponseEntity.ok(response);

    }

    /**
     * 주문 단건 조회
     * @param userId BFF가 JWT에서 추출하여 헤더로 전달한 사용자 ID
     * @param correlationId 분산 추적 ID
     * @param orderId 조회할 주문 ID
     * @return 주문 상세 정보
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Correlation-Id") String correlationId,
            @PathVariable Long orderId
    ) {
        log.debug("주문 단건 조회 요청 - correlationId: {}, userId: {}, orderId: {}",
                correlationId, userId, orderId);

        OrderResponse response = orderService.getOrder(userId, orderId, correlationId);
        return ResponseEntity.ok(response);
    }

    /**
     * 주문 목록 조회 (페이징)
     *
     * @param userId BFF가 JWT에서 추출하여 헤더로 전달한 사용자 ID
     * @param correlationId 분산 추적 ID
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @param status 주문 상태 필터 (선택)
     * @return 페이징된 주문 목록
     */
    @GetMapping
    public ResponseEntity<PageResponse<OrderResponse>> getOrders(
        @RequestHeader("X-User-Id") Long userId,
        @RequestHeader("X-Correlation-Id") String correlationId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String status
    ) {
        log.debug("주문 목록 조회 요청 - correlationId: {}, userId: {}, page: {}, size: {}, status: {}",
                correlationId, userId, page, size, status);

        PageResponse<OrderResponse> response = orderService.getOrders(userId, page, size, status, correlationId);
        return ResponseEntity.ok(response);
    }

}
