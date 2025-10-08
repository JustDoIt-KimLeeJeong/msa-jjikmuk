package com.tradingsystem.order.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderType {
    MARKET("시장가", "현재 시장가로 즉시 체결"),
    LIMIT("지정가", "지정한 가격에 도달하면 체결");

    private final String displayName;
    private final String description;
}
