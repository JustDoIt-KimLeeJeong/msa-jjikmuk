package com.tradingsystem.order.repository;

import com.tradingsystem.order.domain.Order;
import com.tradingsystem.order.domain.OrderStatus;
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
