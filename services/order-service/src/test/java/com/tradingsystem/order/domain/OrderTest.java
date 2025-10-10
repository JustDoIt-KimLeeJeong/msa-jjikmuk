// src/test/java/com/tradingsystem/order/domain/OrderTest.java
package com.tradingsystem.order.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.BeforeEach;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderTest {

    private Order limitBuyOrder;
    private Order marketSellOrder;

    @BeforeEach
    void setUp() {
        // 1. 지정가(LIMIT) 매수 주문 (체결 가능, 만료 시간 있음)
        limitBuyOrder = Order.builder()
                .userId(1L)
                .symbol("005930")
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .price(new BigDecimal("90000"))
                .quantity(100)
                .filledQuantity(0)
                .status(OrderStatus.PENDING)
                .clientOrderId("LIMIT_001")
                .expiresAt(LocalDateTime.now().plusDays(1)) // 하루 뒤 만료
                .build();

        // 2. 시장가(MARKET) 매도 주문 (항상 체결 가능, 만료 시간 없음)
        marketSellOrder = Order.builder()
                .userId(2L)
                .symbol("000660")
                .side(OrderSide.SELL)
                .type(OrderType.MARKET)
                .quantity(50)
                .filledQuantity(0)
                .status(OrderStatus.PENDING)
                .clientOrderId("MARKET_001")
                .build();
    }

    //---------------------------------------------------------
    // 1. 체결 수량 추가 및 상태 업데이트 로직 검증 (addFilledQuantity)
    //---------------------------------------------------------
    @Nested
    @DisplayName("체결 수량 추가 및 상태 업데이트")
    class AddFilledQuantityTest {

        @Test
        @DisplayName("부분 체결 시 상태가 PARTIALLY_FILLED로 변경되어야 한다")
        void testPartialFill() {
            // when
            limitBuyOrder.addFilledQuantity(30);

            // then
            assertThat(limitBuyOrder.getFilledQuantity()).isEqualTo(30);
            assertThat(limitBuyOrder.getStatus()).isEqualTo(OrderStatus.PARTIALLY_FILLED);
            assertThat(limitBuyOrder.getRemainingQuantity()).isEqualTo(70);
        }

        @Test
        @DisplayName("전부 체결 시 상태가 FILLED로 변경되어야 한다")
        void testFullFill() {
            // given: 70개 부분 체결 상태
            limitBuyOrder.addFilledQuantity(70);
            assertThat(limitBuyOrder.getStatus()).isEqualTo(OrderStatus.PARTIALLY_FILLED);

            // when: 남은 30개 전부 체결
            limitBuyOrder.addFilledQuantity(30);

            // then
            assertThat(limitBuyOrder.getFilledQuantity()).isEqualTo(100);
            assertThat(limitBuyOrder.getStatus()).isEqualTo(OrderStatus.FILLED);
            assertThat(limitBuyOrder.getRemainingQuantity()).isEqualTo(0);
        }

        @Test
        @DisplayName("체결 수량이 남은 수량을 초과하면 예외가 발생해야 한다")
        void testOverFillThrowsException() {
            // when, then
            assertThrows(IllegalArgumentException.class, () -> {
                // 주문 수량(100)을 초과하는 101개를 체결 시도
                limitBuyOrder.addFilledQuantity(101);
            }, "체결 수량이 남은 수량을 초과할 때 예외가 발생해야 함");
        }
    }

    //---------------------------------------------------------
    // 2. 만료 여부 및 체결 가능 여부 로직 검증 (isExpired, isTradable)
    //---------------------------------------------------------
    @Nested
    @DisplayName("만료 및 체결 가능 상태 검증")
    class ExpirationAndTradableTest {

        private final LocalDateTime NOW = LocalDateTime.now();

        @Test
        @DisplayName("지정가 주문은 만료 시간이 지나면 isExpired가 true를 반환해야 한다")
        void testLimitOrderIsExpired() {
            // given: 만료 시간이 현재 시간보다 이전인 주문
            Order expiredOrder = limitBuyOrder.toBuilder()
                    .expiresAt(NOW.minusHours(1))
                    .build();

            // then
            assertThat(expiredOrder.isExpired(NOW)).isTrue();
            assertThat(expiredOrder.isTradable(NOW)).isFalse(); // 만료되었으므로 체결 불가능
        }

        @Test
        @DisplayName("시장가 주문은 isExpired가 항상 false를 반환해야 한다")
        void testMarketOrderIsNotExpired() {
            // then
            assertThat(marketSellOrder.isExpired(NOW.plusYears(100))).isFalse(); // 한참 뒤에도 만료 아님
            assertThat(marketSellOrder.isTradable(NOW)).isTrue(); // 체결 가능
        }

        @Test
        @DisplayName("부분 체결된 지정가 주문은 만료되지 않았다면 체결 가능해야 한다")
        void testPartiallyFilledOrderIsTradable() {
            // given: 부분 체결 상태로 변경
            limitBuyOrder.addFilledQuantity(10);

            // then
            // 상태: PARTIALLY_FILLED, 만료되지 않음
            assertThat(limitBuyOrder.isTradable(NOW)).isTrue();
        }

        @Test
        @DisplayName("취소된 주문은 isTradable이 false를 반환해야 한다")
        void testCanceledOrderIsNotTradable() {
            // given
            limitBuyOrder.updateStatus(OrderStatus.CANCELLED);

            // then
            assertThat(limitBuyOrder.isTradable(NOW)).isFalse();
        }
    }

    //---------------------------------------------------------
    // 3. 주문 타입 및 방향 로직 검증
    //---------------------------------------------------------
    @Nested
    @DisplayName("타입 및 방향 확인 메서드")
    class TypeAndSideTest {

        @Test
        @DisplayName("주문 타입 및 방향이 정확해야 한다")
        void testOrderTypeAndSide() {
            // limitBuyOrder
            assertThat(limitBuyOrder.isLimitOrder()).isTrue();
            assertThat(limitBuyOrder.isMarketOrder()).isFalse();
            assertThat(limitBuyOrder.isBuyOrder()).isTrue();
            assertThat(limitBuyOrder.isSellOrder()).isFalse();

            // marketSellOrder
            assertThat(marketSellOrder.isLimitOrder()).isFalse();
            assertThat(marketSellOrder.isMarketOrder()).isTrue();
            assertThat(marketSellOrder.isBuyOrder()).isFalse();
            assertThat(marketSellOrder.isSellOrder()).isTrue();
        }
    }
}