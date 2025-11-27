package com.tradingsystem.order.controller;

import com.tradingsystem.order.dto.request.CreateOrderRequest;
import com.tradingsystem.order.dto.response.OrderResponse;
import com.tradingsystem.order.dto.response.PageResponse;
import com.tradingsystem.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 주문 관리 컨트롤러
 * - 주문 생성/취소/조회 엔드포인트 제공
 * - BFF를 통해 JWT 인증된 요청만 처리
 * - correlationId를 통한 분산 추적 지원
 * - MDC에서 correlationId와 userId를 자동으로 가져옴
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
     * @param request 주문 생성 요청 DTO
     * @return 생성된 주문 정보
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request
            ) {
        String userId = MDC.get("userId");
        String correlationId = MDC.get("correlationId");

        log.info("주문 생성 요청 - correlationId: {}, userId: {}, symbol: {}, side: {}, type: {}",
                correlationId, userId, request.getSymbol(), request.getSide(), request.getType());

        OrderResponse response = orderService.createOrder(userId, request);

        log.info("주문 생성 완료 - orderId: {}, correlationId: {}", response.getOrderId(), correlationId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 주문 취소
     *
     * @param orderId 취소할 주문 ID
     * @return 취소된 주문 정보
     */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long orderId
    ) {

        String userId = MDC.get("userId");
        String correlationId = MDC.get("correlationId");

        log.info("주문 취소 요청 - correlationId: {}, userId: {}, orderId: {}",
                correlationId, userId, orderId);

        OrderResponse response = orderService.cancelOrder(userId, orderId);

        log.info("주문 취소 완료 - orderId: {}, correlationId: {}", orderId, correlationId);

        return ResponseEntity.ok(response);

    }

    /**
     * 주문 단건 조회
     * @param orderId 조회할 주문 ID
     * @return 주문 정보
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable Long orderId
    ) {
        String userId = MDC.get("userId");
        String correlationId = MDC.get("correlationId");

        log.info("주문 단건 조회 요청 - correlationId: {}, userId: {}, orderId: {}",
                correlationId, userId, orderId);

        OrderResponse response = orderService.getOrder(userId, orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * 주문 목록 조회 (페이징)
     *
     * @param pageable 페이징 파라미터 (page, size, sort)
     * @return 페이징된 주문 목록
     */
    @GetMapping
    public ResponseEntity<PageResponse<OrderResponse>> getOrders(
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        String userId = MDC.get("userId");
        String correlationId = MDC.get("correlationId");

        log.info("주문 목록 조회 요청 - correlationId: {}, userId: {}, page: {}, size: {}",
                correlationId, userId, pageable.getPageNumber(), pageable.getPageSize());

        Page<OrderResponse> page = orderService.getOrders(userId, pageable);

        PageResponse<OrderResponse> response = PageResponse.of(page);

        log.info("Fetched {} orders - UserId: {}, TotalElements: {}",
                response.getContent().size(), userId, response.getTotalElements());

        return ResponseEntity.ok(response);
    }

}
