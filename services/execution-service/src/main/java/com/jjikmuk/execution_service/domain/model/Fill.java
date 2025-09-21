package com.jjikmuk.execution_service.domain.model;


import java.math.BigDecimal;
import java.time.Instant;

/**
 * 개별 체결 라인(가격, 수량, 시각)
 */
public record Fill(
    BigDecimal price,
    long qty,
    Instant executedAt
) {}
