package com.tradingsystem.order.config;

import com.tradingsystem.order.domain.Order;
import com.tradingsystem.order.domain.OrderSide;
import com.tradingsystem.order.domain.OrderStatus;
import com.tradingsystem.order.domain.OrderType;
import com.tradingsystem.order.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JpaAuditingTest {

    @Autowired
    private OrderRepository orderRepository;

    @Test
    @DisplayName("JPA Auditing - createdAt 자동 설정")
    void testCreatedAtAutoPopulated() throws InterruptedException {
        // given
        Order order = Order.builder()
                .userId(1L)
                .symbol("005930")
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .price(new BigDecimal(90000))
                .quantity(10)
                .filledQuantity(0)
                .status(OrderStatus.PENDING)
                .clientOrderId("CLIENT_ORDER_AUDIT")
                .build();

        LocalDateTime beforeSave = LocalDateTime.now();
        Thread.sleep(100); // 시간 차이 확보

        // when
        Order saved = orderRepository.save(order);

        // then
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getCreatedAt()).isAfter(beforeSave);
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("JPA Auditing - updatedAt 자동 갱신")
    void testUpdatedAtAutoUpdated() throws InterruptedException {
        // given
        Order order = Order.builder()
                .userId(1L)
                .symbol("005930")
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .price(new BigDecimal(90000))
                .quantity(10)
                .filledQuantity(0)
                .status(OrderStatus.PENDING)
                .clientOrderId("CLIENT_ORDER_UPDATE")
                .build();

        Order saved = orderRepository.saveAndFlush(order);
        LocalDateTime initialUpdatedAt = saved.getUpdatedAt();
        LocalDateTime initialCreatedAt = saved.getCreatedAt();

        Thread.sleep(100); // 시간 차이 확보

        // when
        saved.updateStatus(OrderStatus.ACCEPTED);
        Order updated = orderRepository.saveAndFlush(saved);

        // then
        assertThat(updated.getUpdatedAt()).isNotEqualTo(initialUpdatedAt);
        assertThat(updated.getCreatedAt()).isEqualTo(initialCreatedAt); // createdAt은 불변
    }
}