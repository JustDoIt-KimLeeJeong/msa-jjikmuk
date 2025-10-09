package com.jjikmuk.execution_service.infrastructure.persistence.repository;

import com.jjikmuk.execution_service.domain.model.Order;
import com.jjikmuk.execution_service.infrastructure.persistence.entity.OrderOpenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
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

    /**
     * 특정 심볼에 대한 모든 오픈 주문을 조회합니다.
     */
    List<OrderOpenEntity> findBySymbol(String symbol);

    /**
     * 지정된 가격보다 높거나 같은 매수 주문을 가격 우선(내림차순), 시간 우선(오름차순)으로 조회합니다.
     */
    List<OrderOpenEntity> findAllBySymbolAndSideAndPriceGreaterThanEqualOrderByPriceDescArrivalSeqAsc(String symbol, Order.Side side, java.math.BigDecimal price);

    /**
     * 지정된 가격보다 낮거나 같은 매도 주문을 가격 우선(오름차순), 시간 우선(오름차순)으로 조회합니다.
     */
    List<OrderOpenEntity> findAllBySymbolAndSideAndPriceLessThanEqualOrderByPriceAscArrivalSeqAsc(String symbol, Order.Side side, java.math.BigDecimal price);

}
