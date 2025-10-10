package com.tradingsystem.order.repository;

import com.tradingsystem.order.domain.OrderTrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderTradeRepository extends JpaRepository<OrderTrade, Long> {

    /**
     * 주문 별 체결 이력 조회 (시간순)
     */
    List<OrderTrade> findByOrderIdOrderByExecutedAtAsc(Long orderId);

    /**
     * tradeId 기반 중복 체크 (멱등성 보장)
     */
    Optional<OrderTrade> findByTradeId(String tradeId);

    /**
     * 주문 별 총 체결 수량 계산
     */
    @Query("SELECT COALESCE(SUM(ot.quantity), 0) FROM OrderTrade ot " +
           "WHERE ot.orderId = :orderId" )
    BigDecimal calculateTotalFilledQuantity(@Param("orderId") Long orderId);

    /**
     * 주문별 평균 체결가 계산
     * (체결가 * 수량)의 합 / 총 수량
     */
    @Query("SELECT COALESCE(SUM(ot.price * ot.quantity) / SUM(ot.quantity), 0) " +
            "FROM OrderTrade ot WHERE ot.orderId = :orderId")
    BigDecimal calculateAveragePrice(@Param("orderId") Long orderId);



}
