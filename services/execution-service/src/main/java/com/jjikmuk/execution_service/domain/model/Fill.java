package com.jjikmuk.execution_service.domain.model;


import com.jjikmuk.execution_service.domain.model.value.OrderId;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 개별 체결 라인(가격, 수량, 시각)
 */
public record Fill(
    OrderId orderId,
    BigDecimal price,
    long qty,
    Instant executedAt
) {}
