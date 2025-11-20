package com.tradingsystem.order.exception;

/**
 * CorrelationId가 MDC에 없을 때 발생하는 예외
 * - BFF를 거치지 않은 직접 호출 시 발생
 * - 내부 API는 반드시 BFF를 통해서만 호출되어야 함
 */
public class CorrelationIdMissingException extends RuntimeException {

  public CorrelationIdMissingException(String message) {
    super(message);
  }
}