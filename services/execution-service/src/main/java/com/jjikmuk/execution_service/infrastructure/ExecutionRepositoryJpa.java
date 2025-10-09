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

    @Override
    public void insertOpen(Order order) {
        OrderOpenEntity entity = mapper.toOpenOrderEntity(order);
        orderOpenJpaRepository.save(entity);
    }

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
        return entityOpt.map(mapper::toDomain);
    }



    @Override
    public long nextArrivalSeq(Symbol symbol) {
        // TODO: SymbolSeqAdapter로 위임해야 함. 현재는 ExecutionFacade에서 직접 호출하므로 여기서는 불필요.
        return 0;
    }


    @Override
    public boolean deleteOpenIfExists(OrderId orderId) {
        if (orderOpenJpaRepository.existsById(orderId.value())) {
            orderOpenJpaRepository.deleteById(orderId.value());
            return true;
        }
        return false;
    }

    @Override
    public void removeOpen(Order order) {
        orderOpenJpaRepository.deleteById(order.getOrderId().value());
    }

    @Override
    public Optional<Order> findOpen(OrderId id) {
        return orderOpenJpaRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public List<Order> findAllOpenOrdersBySymbol(Symbol symbol) {
        return orderOpenJpaRepository.findBySymbol(symbol.value())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void upsertTradeAndAppendFills(OrderId orderId, Order.Side side, Symbol symbol, List<Fill> fills, long leavesQty, Instant now) {
        // 1. orderId로 기존 Trade를 찾거나, 없으면 새로 생성
        TradeEntity tradeEntity = tradeJpaRepository.findByOrderId(orderId.value())
                .orElseGet(() -> new TradeEntity(orderId.value(), symbol.value(), side, leavesQty));

        // 2. 새로운 Fill 들을 Trade에 추가
        for (Fill fill : fills) {
            FillEntity fillEntity = mapper.toFillEntity(fill, tradeEntity);
            tradeEntity.addFill(fillEntity);
        }

        // 3. Trade의 최종 잔량 업데이트
        tradeEntity.setLeavesQty(leavesQty);

        // 4. 변경된 Trade와 새로운 Fill들 저장
        tradeJpaRepository.save(tradeEntity);


    }
}
