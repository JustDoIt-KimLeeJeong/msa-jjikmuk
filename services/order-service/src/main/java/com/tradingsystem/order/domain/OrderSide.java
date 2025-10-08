package com.tradingsystem.order.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 주문의 매수/매도 방향을 나타내는 Enum 클래스입니다.
 */

@Getter
@RequiredArgsConstructor
public enum OrderSide {
    BUY("매수", 1),
    SELL("매도", -1);

    private final String displayName; // UI에 표시될 이름 (예: "매수", "매도")
    private final int multiplier;  // 주문 수량 계산에 사용되는 승수(매수(BUY)는 1, 매도(SELL)는 -1 값)
}
