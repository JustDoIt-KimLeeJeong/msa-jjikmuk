package com.tradingsystem.order.exception;

import com.tradingsystem.order.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 전역 예외 처리 핸들러
 * - 모든 컨트롤러의 예외를 통합 처리
 * - 일관된 에러 응답 포맷 제공
 * - 적절한 HTTP 상태코드 매핑
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 중복 주문 예외 처리
     * HTTP 409 Conflict
     */
    @ExceptionHandler(DuplicateOrderException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateOrder(DuplicateOrderException e) {
        log.warn("중복 주문 예외: {}", e.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of("DUPLICATE_ORDER", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * 주문 미발견 예외 처리
     * HTTP 404 Not Found
     */
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException e) {
        log.warn("주문 미발견 예외: {}", e.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of("ORDER_NOT_FOUND", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * 잘못된 인자 예외 처리 (비즈니스 로직 검증)
     * HTTP 400 Bad Request
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("잘못된 요청: {}", e.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of("INVALID_REQUEST", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * 잘못된 상태 예외 처리 (취소 불가능한 주문 등)
     * HTTP 400 Bad Request
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException e) {
        log.warn("잘못된 상태: {}", e.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of("INVALID_STATE", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Validation 예외 처리 (@Valid 실패)
     * HTTP 400 Bad Request
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.warn("Validation 오류: {}", message);

        ErrorResponse errorResponse = ErrorResponse.of("VALIDATION_ERROR", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * 예상치 못한 예외 처리
     * HTTP 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception e) {
        log.error("예상치 못한 오류", e);

        ErrorResponse errorResponse = ErrorResponse.of("INTERNAL_ERROR", "서버 내부 오류가 발생했습니다");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(UnauthorizedOrderAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedAccess(
            UnauthorizedOrderAccessException e) {
        log.warn("Unauthorized order access: {}", e.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .code("UNAUTHORIZED_ORDER_ACCESS")
                .message("주문에 접근 권한이 없습니다.")
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * 주문 상태 예외 처리
     * - 유효하지 않은 상태 변경 시 (예: 이미 완료된 주문 취소 등)
     * HTTP 400 Bad Request
     */
    @ExceptionHandler(InvalidOrderStatusException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOrderStatus(InvalidOrderStatusException e) {
        log.warn("잘못된 주문 상태 변경 시도: {}", e.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of("INVALID_ORDER_STATUS", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * CorrelationId 누락 예외 처리
     * - 필수 헤더(또는 MDC) 누락 (BFF 미경유 호출 등)
     * HTTP 400 Bad Request (또는 정책에 따라 403 Forbidden)
     */
    @ExceptionHandler(CorrelationIdMissingException.class)
    public ResponseEntity<ErrorResponse> handleCorrelationIdMissing(CorrelationIdMissingException e) {
        // 보안/인프라 정책 위반일 수 있으므로 Error 레벨로 로깅하거나 Warn 유지
        log.warn("CorrelationId 누락됨 (BFF 미경유 의심): {}", e.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of("CORRELATION_ID_MISSING", "잘못된 접근입니다. (Missing Correlation ID)");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}
