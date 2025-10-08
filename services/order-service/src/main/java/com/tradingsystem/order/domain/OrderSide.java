package com.tradingsystem.order.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderSide {
    BUY("매수", 1),
    SELL("매도", -1);

    private final String displayName;
    private final int multiplier;  // 수량 계산용
}