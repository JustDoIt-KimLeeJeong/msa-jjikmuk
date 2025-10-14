package com.tradingsystem.order.repository;

import com.tradingsystem.order.domain.Order;
import com.tradingsystem.order.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 주문 ID + 사용자 ID로 조회 (소유권 검증용)
     * - OrderService.getOrder()에서 사용
     * - OrderService.cancelOrder()에서 사용
     */
    Optional<Order> findByIdAndUserId(Long id, Long userId);

    /**
     * 중복 주문 존재 여부 체크 (멱등성 보장)
     * - OrderService.createOrder()에서 사용
     */
    boolean existsByUserIdAndClientOrderId(Long userId, String clientOrderId);

    /**
     * 사용자별 주문 목록 조회 (페이징)
     * - OrderService.getOrders()에서 사용
     */
    Page<Order> findByUserId(Long userId, Pageable pageable);

    /**
     * 사용자 + 상태별 주문 목록 조회 (페이징)
     * - OrderService.getOrders()에서 사용
     */
    Page<Order> findByUserIdAndStatus(Long userId, OrderStatus status, Pageable pageable);




    /**
     * 사용자별 주문 조회 (최신순)
     */
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 사용자 + 상태별 주문 조회
     */
    List<Order> findByUserIdAndStatus(Long userId, OrderStatus status);

    /**
     * clientOrderId 기반 중복 체크 (멱등성 보장)
     */
    Optional<Order> findByUserIdAndClientOrderId(Long userId, String clientOrderId);

    /**
     * 만료 대상 지정가 주문 조회
     * - 상태 : PENDING 또는 ACCEPTED
     * - 만료 시간 < 현재 시간
     */
    @Query("SELECT o FROM Order o WHERE o.status IN :statuses " +
            "AND o.expiresAt IS NOT NULL AND o.expiresAt < :now")
    List<Order> findExpiredOrders(
            @Param("statuses") List<OrderStatus> statuses,
            @Param("now") LocalDateTime now
    );

    /**
     * 예약 주문 조회 (장시간 외 생성된 주문)
     * - 상태 : RESERVED
     */
    List<Order> findByStatus(OrderStatus status);

    /**
     * 종목 별 활성 주문 조회 (체결 가능한 주문)
     */
    @Query("SELECT o FROM Order o WHERE o.symbol = :symbol " +
            "AND o.status IN :statuses")
    List<Order> findActiveOrdersBySymbol(
            @Param("symbol") String symbol,
            @Param("statuses") List<OrderStatus> statuses
    );


}
