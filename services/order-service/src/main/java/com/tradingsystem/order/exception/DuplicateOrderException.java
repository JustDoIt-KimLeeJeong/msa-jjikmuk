package com.tradingsystem.order.exception;

/**
 * 중복 주문 예외
 * - clientOrderId 기반 멱등성 보장
 */
public class DuplicateOrderException extends RuntimeException {
    public DuplicateOrderException(String message) {
        super(message);
    }
    public DuplicateOrderException(String message, Throwable cause) {
        super(message, cause);
    }

}
