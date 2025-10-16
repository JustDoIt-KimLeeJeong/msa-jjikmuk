package com.jjikmuk.execution_service.infrastructure;

import com.jjikmuk.execution_service.domain.model.Fill;
import com.jjikmuk.execution_service.domain.model.Order;
import com.jjikmuk.execution_service.domain.model.value.OrderId;
import com.jjikmuk.execution_service.domain.model.value.Symbol;
import com.jjikmuk.execution_service.domain.port.ExecutionRepository;
import com.jjikmuk.execution_service.infrastructure.persistence.entity.FillEntity;
import com.jjikmuk.execution_service.infrastructure.persistence.entity.OrderOpenEntity;
import com.jjikmuk.execution_service.infrastructure.persistence.entity.TradeEntity;
import com.jjikmuk.execution_service.infrastructure.persistence.mapper.PersistenceMapper;
import com.jjikmuk.execution_service.infrastructure.persistence.repository.OrderOpenJpaRepository;
import com.jjikmuk.execution_service.infrastructure.persistence.repository.TradeJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ExecutionRepositoryJpa implements ExecutionRepository {

    private final OrderOpenJpaRepository orderOpenJpaRepository;
    private final PersistenceMapper mapper;
    private final TradeJpaRepository tradeJpaRepository;

    /**
     * 미체결 주문을 저장합니다.
     * 도메인 Order → JPA 엔티티로 변환 후 영속화합니다.
     */
    @Override
    public void insertOpen(Order order) {
        // 도메인 → 엔티티 매핑
        OrderOpenEntity entity = mapper.toOpenOrderEntity(order);
        orderOpenJpaRepository.save(entity);
    }

    /**
     * 들어온 주문의 반대 사이드에서, 가격/도착순번 기준으로
     * 가장 유리한(최적의) 상대 주문을 1건 조회합니다.
     *
     * BUY가 들어오면: 가장 싼 SELL (가격 오름차순) → arrivalSeq 오름차순
     * SELL이 들어오면: 가장 비싼 BUY (가격 내림차순) → arrivalSeq 오름차순
     */
    @Override
    public Optional<Order> peekBestOpposite(Symbol symbol, Order.Side incomingSide) {
        Optional<OrderOpenEntity> entityOpt;
        if (incomingSide == Order.Side.BUY) {
            // 매수 주문에 대한 최적의 상대는 가장 저렴한 매도 주문
            entityOpt = orderOpenJpaRepository.findFirstBySymbolAndSideOrderByPriceAscArrivalSeqAsc(symbol.value(), Order.Side.SELL);
        } else {
            // 매도 주문에 대한 최적의 상대는 가장 비싼 매수 주문
            entityOpt = orderOpenJpaRepository.findFirstBySymbolAndSideOrderByPriceDescArrivalSeqAsc(symbol.value(), Order.Side.BUY);
        }
        // 엔티티 → 도메인 매핑 후 반환
        return entityOpt.map(mapper::toDomain);
    }



    @Override
    public long nextArrivalSeq(Symbol symbol) {
        // TODO: SymbolSeqAdapter로 위임해야 함. 현재는 ExecutionFacade에서 직접 호출하므로 여기서는 불필요.
        return 0;
    }

    /**
     * 주어진 주문이 존재하면 미체결에서 삭제하고 true, 없으면 false를 반환합니다.
     */
    @Override
    public boolean deleteOpenIfExists(OrderId orderId) {
        if (orderOpenJpaRepository.existsById(orderId.value())) {
            orderOpenJpaRepository.deleteById(orderId.value());
            return true;
        }
        return false;
    }

    /**
     * 미체결 주문을 강제 삭제합니다.
     */
    @Override
    public void removeOpen(Order order) {
        orderOpenJpaRepository.deleteById(order.getOrderId().value());
    }

    /**
     * 미체결 주문의 잔량(leavesQty)만 갱신합니다.
     * (가격/사이드/심볼 등 다른 속성은 변경하지 않습니다)
     */
    @Override
    public void updateOpen(Order order) {
        orderOpenJpaRepository.findById(order.getOrderId().value()).ifPresent(entity -> {
            entity.setLeavesQty(order.getLeavesQty());  // 잔량 업데이트
            orderOpenJpaRepository.save(entity);        // 저장
        });
    }

    /**
     * 미체결 주문 단건 조회 (ID 기준).
     */
    @Override
    public Optional<Order> findOpen(OrderId id) {
        return orderOpenJpaRepository.findById(id.value()).map(mapper::toDomain);
    }

    /**
     * 특정 심볼의 모든 미체결 주문을 조회합니다.
     * (주의: 운영환경에서는 페이징/상한 적용을 고려하세요)
     */
    @Override
    public List<Order> findAllOpenOrdersBySymbol(Symbol symbol) {
        return orderOpenJpaRepository.findBySymbol(symbol.value())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Order> findMatchingBuyOrders(Symbol symbol, java.math.BigDecimal askPrice) {
        return orderOpenJpaRepository.findAllBySymbolAndSideAndPriceGreaterThanEqualOrderByPriceDescArrivalSeqAsc(symbol.value(), Order.Side.BUY, askPrice)
            .stream()
            .map(mapper::toDomain)
            .toList();
    }

    /**
     * 매수 매칭 후보 조회:
     * 현재 판매호가(askPrice) 이상을 제시한 모든 BUY 주문을
     * 가격 내림차순 → 도착순번 오름차순으로 반환합니다.
     */
    @Override
    public List<Order> findMatchingSellOrders(Symbol symbol, java.math.BigDecimal bidPrice) {
        return orderOpenJpaRepository.findAllBySymbolAndSideAndPriceLessThanEqualOrderByPriceAscArrivalSeqAsc(symbol.value(), Order.Side.SELL, bidPrice)
            .stream()
            .map(mapper::toDomain)
            .toList();
    }

    /**
     * Trade를 업서트(upsert)하고, 신규 Fill들을 Trade에 추가합니다.
     *
     * <p>동일 orderId에 대한 Trade가 이미 있으면 재사용하고,
     * 없으면 tradeId로 새 엔티티를 생성합니다. 이후 Fill을 모두 append하고
     * 최종 잔량(leavesQty)을 갱신합니다.
     *
     * @param tradeId   체결 그룹 식별자(동일 체결 묶음)
     * @param orderId   주문 ID
     * @param side      해당 주문의 사이드 (BUY/SELL)
     * @param symbol    심볼
     * @param fills     새로 발생한 체결 목록
     * @param leavesQty 체결 후 잔량
     * @param now       체결 기준 시각(필요 시 엔티티 타임스탬프에 사용)
     */
    @Override
    public void upsertTradeAndAppendFills(String tradeId, OrderId orderId, Order.Side side, Symbol symbol, List<Fill> fills, long leavesQty, Instant now) {
        // 1. orderId로 기존 Trade를 찾거나, 없으면 새로 생성
        TradeEntity tradeEntity = tradeJpaRepository.findByOrderId(orderId.value())
                .orElseGet(() -> new TradeEntity(tradeId, orderId.value(), symbol.value(), side, leavesQty));

        // 2. 새로운 Fill 들을 Trade에 추가
        for (Fill fill : fills) {
            FillEntity fillEntity = mapper.toFillEntity(fill, tradeEntity, tradeId);
            tradeEntity.addFill(fillEntity);
        }

        // 3. Trade의 최종 잔량 업데이트
        tradeEntity.setLeavesQty(leavesQty);

        // 4. 변경된 Trade와 새로운 Fill들 저장
        tradeJpaRepository.save(tradeEntity);


    }
}
