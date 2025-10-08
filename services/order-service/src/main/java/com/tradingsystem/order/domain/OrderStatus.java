package com.tradingsystem.order.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 주문의 생명주기에 따른 상태를 관리하는 Enum 클래스입니다.
 * PENDING -> (RESERVED) -> ACCEPTED -> PARTIALLY_FILLED -> FILLED
 * 각 단계에서 REJECTED, CANCELLED, EXPIRED 상태로 변경될 수 있습니다.
 */

@Getter
@RequiredArgsConstructor
public enum OrderStatus {
    PENDING("대기중", "주문이 생성되었습니다"),
    RESERVED("예약됨", "장 시간 외 예약 주문"),
    ACCEPTED("승인됨", "예약이 승인되어 체결 대기 중입니다"),
    REJECTED("거부됨", "잔고 부족 등으로 주문이 거부되었습니다"),
    PARTIALLY_FILLED("부분체결", "주문이 일부만 체결되었습니다"),
    FILLED("체결완료", "주문이 완전히 체결되었습니다"),
    CANCELLED("취소됨", "사용자가 주문을 취소했습니다"),
    EXPIRED("만료됨", "24시간 경과로 주문이 만료되었습니다");

    private final String displayName; // UI에 표시될 이름
    private final String description; // 상태에 대한 상세 설명
}
