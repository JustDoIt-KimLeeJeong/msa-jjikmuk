package com.jjikmuk.execution_service.infrastructure.persistence.repository;

import com.jjikmuk.execution_service.domain.model.Order;
import com.jjikmuk.execution_service.infrastructure.persistence.entity.OrderOpenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderOpenJpaRepository extends JpaRepository<OrderOpenEntity, String> {

    /**
     * 가장 유리한 매도 주문(가장 낮은 가격, 가장 빠른 도착)을 찾습니다.
     */
    Optional<OrderOpenEntity> findFirstBySymbolAndSideOrderByPriceAscArrivalSeqAsc(String symbol, Order.Side side);

    /**
     * 가장 유리한 매수 주문(가장 높은 가격, 가장 빠른 도착)을 찾습니다.
     */
    Optional<OrderOpenEntity> findFirstBySymbolAndSideOrderByPriceDescArrivalSeqAsc(String symbol, Order.Side side);

}
