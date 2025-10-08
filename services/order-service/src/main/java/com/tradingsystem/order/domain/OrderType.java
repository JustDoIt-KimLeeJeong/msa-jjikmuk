package com.tradingsystem.order.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 주문 유형(시장가, 지정가)을 정의하는 Enum 클래스입니다.
 */

@Getter
@RequiredArgsConstructor
public enum OrderType {
    MARKET("시장가", "현재 시장가로 즉시 체결"),
    LIMIT("지정가", "지정한 가격에 도달하면 체결");

    private final String displayName; // UI에 표시될 이름 (예: "시장가", "지정가")
    private final String description; // 주문 유형에 대한 상세 설명
}

