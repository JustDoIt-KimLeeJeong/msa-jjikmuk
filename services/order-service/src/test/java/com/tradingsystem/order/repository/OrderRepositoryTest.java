package com.tradingsystem.order.repository;

import com.tradingsystem.order.config.TestJpaConfig;
import com.tradingsystem.order.domain.Order;
import com.tradingsystem.order.domain.OrderSide;
import com.tradingsystem.order.domain.OrderStatus; // Enum 사용
import com.tradingsystem.order.domain.OrderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
@ActiveProfiles("test")
@Import(TestJpaConfig.class)
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        // 한국 주식 기준: 005930 (삼성전자) 및 정수 가격 사용
        sampleOrder = Order.builder()
                .userId("1L")
                .symbol("005930")
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .price(new BigDecimal("90000")) // 90,000원
                .quantity(10)
                .filledQuantity(0)
                .status(OrderStatus.PENDING)
                .clientOrderId("CLIENT_ORDER_001")
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
    }

    // --- 기본 CRUD 및 멱등성 검증 ---

    @Test
    @DisplayName("주문 저장 및 조회 테스트 (CRUD 기본)")
    void testSaveAndFindOrder() {
        // given
        Order saved = orderRepository.save(sampleOrder);

        // when
        Optional<Order> found = orderRepository.findById(saved.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getSymbol()).isEqualTo("005930");
    }

    @Test
    @DisplayName("clientOrderId 기반 중복 체크 (멱등성)")
    void testFindByUserIdAndClientOrderId() {
        // given
        orderRepository.save(sampleOrder);

        // when
        Optional<Order> found = orderRepository
                .findByUserIdAndClientOrderId("1L", "CLIENT_ORDER_001");

        // then
        assertThat(found).isPresent();
    }

    // --- 커스텀 조회 메서드 검증 ---

    @Test
    @DisplayName("사용자별 주문 목록 조회 (최신순)")
    void testFindByUserIdOrderByCreatedAtDesc() {
        // given
        orderRepository.save(sampleOrder); // 먼저 저장

        // 000660 (SK하이닉스) 주문 - 나중에 저장 (최신)
        Order secondOrder = Order.builder()
                .userId("1L")
                .symbol("000660")
                .side(OrderSide.SELL)
                .type(OrderType.MARKET)
                .quantity(5)
                .filledQuantity(0)
                .status(OrderStatus.PENDING)
                .clientOrderId("CLIENT_ORDER_002")
                .build();
        orderRepository.save(secondOrder); // 나중에 저장됨

        // when
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc("1L");

        // then
        assertThat(orders).hasSize(2);
        // 최신순 (DESC)이므로, 나중에 저장된 000660이 첫 번째에 와야 함
        assertThat(orders.get(0).getSymbol()).isEqualTo("000660");
    }

    @Test
    @DisplayName("사용자 + 상태별 주문 조회 (FILLED, PENDING)")
    void testFindByUserIdAndStatus() {
        // given
        // 035720 (카카오) 주문 - FILLED 상태
        Order filledOrder = Order.builder()
                .userId("1L")
                .symbol("035720")
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(10)
                .filledQuantity(10)
                .status(OrderStatus.FILLED) // 👈 FILLED 상태 사용
                .clientOrderId("CLIENT_ORDER_003")
                .build();
        orderRepository.save(filledOrder);
        // sampleOrder는 005930, PENDING 상태
        orderRepository.save(sampleOrder);

        // when
        List<Order> pendingOrders = orderRepository.findByUserIdAndStatus("1L", OrderStatus.PENDING);
        List<Order> filledOrders = orderRepository.findByUserIdAndStatus("1L", OrderStatus.FILLED); // 👈 FILLED 상태로 조회

        // then
        assertThat(pendingOrders).hasSize(1);
        assertThat(pendingOrders.get(0).getSymbol()).isEqualTo("005930");

        assertThat(filledOrders).hasSize(1);
        assertThat(filledOrders.get(0).getSymbol()).isEqualTo("035720");
    }

    @Test
    @DisplayName("예약 주문 (RESERVED) 조회")
    void testFindByStatusReserved() {
        // given
        Order reservedOrder = Order.builder()
                .userId("1L")
                .symbol("005930")
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(10)
                .filledQuantity(0)
                .status(OrderStatus.RESERVED) // 👈 RESERVED 상태 사용
                .clientOrderId("RESERVED_ORDER")
                .build();
        orderRepository.save(reservedOrder);

        // when
        List<Order> reserved = orderRepository.findByStatus(OrderStatus.RESERVED);

        // then
        assertThat(reserved).hasSize(1);
        assertThat(reserved.get(0).getStatus()).isEqualTo(OrderStatus.RESERVED);
    }

    @Test
    @DisplayName("만료 대상 주문 (EXPIRED) 조회")
    void testFindExpiredOrders() {
        // given
        // 만료 시간이 현재 시간보다 이전인 주문 (만료 대상)
        Order expiredOrder = Order.builder()
                .userId("1L")
                .symbol("005930")
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .price(new BigDecimal("90000"))
                .quantity(10)
                .filledQuantity(0)
                .status(OrderStatus.ACCEPTED)
                .clientOrderId("EXPIRED_ORDER")
                .expiresAt(LocalDateTime.now().minusHours(1)) // 이미 만료됨
                .build();
        orderRepository.save(expiredOrder);

        // when
        List<Order> expired = orderRepository.findExpiredOrders(
                List.of(OrderStatus.PENDING, OrderStatus.ACCEPTED),
                LocalDateTime.now()
        );

        // then
        assertThat(expired).hasSize(1);
        assertThat(expired.get(0).getClientOrderId()).isEqualTo("EXPIRED_ORDER");
    }

    @Test
    @DisplayName("종목 별 활성 주문 조회 (PENDING, ACCEPTED 상태만)")
    void testFindActiveOrdersBySymbol() {
        // given
        // 005930 (삼성전자) - REJECTED 상태 (비활성)
        Order rejectedOrder = Order.builder()
                .userId("2L")
                .symbol("005930") // 동일 종목
                .side(OrderSide.SELL)
                .type(OrderType.LIMIT)
                .quantity(5)
                .filledQuantity(0)
                .status(OrderStatus.REJECTED) // 👈 비활성 상태 (제외되어야 함)
                .clientOrderId("REJECTED_ORDER")
                .price(new BigDecimal("89000"))
                .build();
        orderRepository.save(rejectedOrder);

        orderRepository.save(sampleOrder); // 005930, PENDING (활성)

        // when
        // 활성 상태 목록 (PENDING, ACCEPTED, PARTIALLY_FILLED)
        List<OrderStatus> activeStatuses = List.of(
                OrderStatus.PENDING,
                OrderStatus.ACCEPTED,
                OrderStatus.PARTIALLY_FILLED
        );

        // 005930 종목의 활성 주문 조회
        List<Order> activeOrders = orderRepository.findActiveOrdersBySymbol("005930", activeStatuses);

        // then
        // 활성 주문은 PENDING 상태인 sampleOrder 1건만 조회되어야 함 (REJECTED는 제외)
        assertThat(activeOrders).hasSize(1);
        assertThat(activeOrders.get(0).getClientOrderId()).isEqualTo("CLIENT_ORDER_001");
        assertThat(activeOrders.get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
    }
}