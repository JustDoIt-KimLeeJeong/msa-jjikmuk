package com.tradingsystem.order.exception;

/**
 * 주문 미발견 예외
 * - 존재하지 않는 주문 조회 시
 * - 소유권 검증 실패 시
 */
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String message) {
        super(message);
    }
    public OrderNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
