package com.jjikmuk.execution_service.domain.service;

import com.jjikmuk.execution_service.domain.model.Fill;
import com.jjikmuk.execution_service.domain.model.Order;
import com.jjikmuk.execution_service.domain.model.value.OrderId;
import com.jjikmuk.execution_service.domain.model.value.Symbol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MatchingEngineTest {

    private static final Symbol SYMBOL = new Symbol("TEST.TICK");

    private MatchingEngine matchingEngine;

    @BeforeEach
    void setUp() {
        matchingEngine = new MatchingEngine();
    }

    @Test
    @DisplayName("서로 다른 심볼이면 매칭은 발생하지 않는다(예외 없음)")
    void match_shouldNotFill_whenSymbolsDiffer() {
        Order buyOrder = createLimitOrder(Order.Side.BUY, "100.00",5);
        Order sellOrder = Order.builder()
                .orderId(new OrderId(java.util.UUID.randomUUID().toString()))
                .symbol(new Symbol("OTHER.TICK")) // 다른 심볼
                .side(Order.Side.SELL)
                .type(Order.Type.LIMIT)
                .price(new BigDecimal("100.00"))
                .origQty(5)
                .leavesQty(5)
                .tif(Order.Tif.GFD)
                .arrivalSeq(1L)
                .createdAt(Instant.now())
                .build();

        Optional<Fill> fill = matchingEngine.match(buyOrder, sellOrder, Instant.now());

        assertThat(fill).isEmpty();              // 매칭 없음
        assertThat(buyOrder.getLeavesQty()).isEqualTo(5);  // 수량 변동 없음
        assertThat(sellOrder.getLeavesQty()).isEqualTo(5);
    }


    @Test
    @DisplayName("가격 조건이 맞지 않으면(cross되지 않으면) 체결은 일어나지 않는다")
    void match_shouldNotFill_whenPriceDoesNotCross() {
        // given
        Order buyOrder = createLimitOrder(Order.Side.BUY, "100.00", 10);
        Order sellOrder = createLimitOrder(Order.Side.SELL, "101.00", 10);

        // when
        Optional<Fill> fillOpt = matchingEngine.match(buyOrder, sellOrder, Instant.now());

        // then
        assertThat(fillOpt).isEmpty();
        assertThat(buyOrder.getLeavesQty()).isEqualTo(10);
        assertThat(sellOrder.getLeavesQty()).isEqualTo(10);
    }

    @Test
    @DisplayName("수량이 동일한 두 지정가 주문은 전량 체결된다")
    void match_shouldFullyFill_whenQuantitiesAreSame() {
        // given
        Order buyOrder = createLimitOrder(Order.Side.BUY, "100.00", 10);
        Order sellOrder = createLimitOrder(Order.Side.SELL, "100.00", 10);
        Instant now = Instant.now();

        // when
        Optional<Fill> fillOpt = matchingEngine.match(buyOrder, sellOrder, now);

        // then
        assertThat(fillOpt).isPresent();
        Fill fill = fillOpt.get();
        assertThat(fill.price()).isEqualByComparingTo("100.00");
        assertThat(fill.qty()).isEqualTo(10);
        assertThat(fill.executedAt()).isEqualTo(now);

        assertThat(buyOrder.getLeavesQty()).isZero();
        assertThat(sellOrder.getLeavesQty()).isZero();
    }

    @Test
    @DisplayName("매수 잔량이 매도 잔량보다 많으면, 매도 잔량만큼 부분 체결된다")
    void match_shouldPartiallyFill_whenBuyQtyIsLarger() {
        // given
        Order buyOrder = createLimitOrder(Order.Side.BUY, "100.00", 10);
        Order sellOrder = createLimitOrder(Order.Side.SELL, "100.00", 5);

        // when
        Optional<Fill> fillOpt = matchingEngine.match(buyOrder, sellOrder, Instant.now());

        // then
        assertThat(fillOpt).isPresent();
        Fill fill = fillOpt.get();
        assertThat(fill.qty()).isEqualTo(5);

        assertThat(buyOrder.getLeavesQty()).isEqualTo(5);
        assertThat(sellOrder.getLeavesQty()).isZero();
    }

    @Test
    @DisplayName("매도 잔량이 매수 잔량보다 많으면, 매수 잔량만큼 부분 체결된다")
    void match_shouldPartiallyFill_whenSellQtyIsLarger() {
        // given
        Order buyOrder = createLimitOrder(Order.Side.BUY, "100.00", 5);
        Order sellOrder = createLimitOrder(Order.Side.SELL, "100.00", 10);

        // when
        Optional<Fill> fillOpt = matchingEngine.match(buyOrder, sellOrder, Instant.now());

        // then
        assertThat(fillOpt).isPresent();
        Fill fill = fillOpt.get();
        assertThat(fill.qty()).isEqualTo(5);

        assertThat(buyOrder.getLeavesQty()).isZero();
        assertThat(sellOrder.getLeavesQty()).isEqualTo(5);
    }

    @Test
    @DisplayName("시장가 매수 주문은 지정가 매도 주문과 체결된다")
    void match_shouldFill_whenMarketBuyMeetsLimitSell() {
        // given
        Order marketBuyOrder = createMarketOrder(Order.Side.BUY, 10);
        Order limitSellOrder = createLimitOrder(Order.Side.SELL, "120.00", 10);

        // when
        Optional<Fill> fillOpt = matchingEngine.match(marketBuyOrder, limitSellOrder, Instant.now());

        // then
        assertThat(fillOpt).isPresent();
        Fill fill = fillOpt.get();
        assertThat(fill.price()).isEqualByComparingTo("120.00"); // 체결가는 지정가(Maker)를 따름
        assertThat(fill.qty()).isEqualTo(10);

        assertThat(marketBuyOrder.getLeavesQty()).isZero();
        assertThat(limitSellOrder.getLeavesQty()).isZero();
    }

    // -- 헬퍼 메소드 -- //

    // 지정가
    private Order createLimitOrder(Order.Side side, String price, long qty) {
        return Order.builder()
            .orderId(new OrderId(java.util.UUID.randomUUID().toString()))
            .symbol(SYMBOL)
            .side(side)
            .type(Order.Type.LIMIT)
            .price(new BigDecimal(price))
            .origQty(qty)
            .leavesQty(qty)
            .tif(Order.Tif.GFD)
            .arrivalSeq(1L)
            .createdAt(Instant.now())
            .build();
    }

    // 시장가
    private Order createMarketOrder(Order.Side side, long qty) {
        return Order.builder()
            .orderId(new OrderId(java.util.UUID.randomUUID().toString()))
            .symbol(SYMBOL)
            .side(side)
            .type(Order.Type.MARKET)
            .price(null)
            .origQty(qty)
            .leavesQty(qty)
            .tif(Order.Tif.IOC)
            .arrivalSeq(1L)
            .createdAt(Instant.now())
            .build();
    }
}