package com.tradingsystem.order.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

/**
 * Correlation ID 검증 및 추출 유틸리티
 * - BFF에서 생성한 UUID v7 기반 Correlation ID 검증
 * - 형식: PREFIX_UUID_v7 (예: ORD_018e8c7a-9c5e-7000-8000-123456789abc)
 *
 * 사용처:
 * 1. Interceptor/Filter - HTTP Header 검증
 * 2. Kafka Consumer - 이벤트 수신 시 검증
 * 3. 로깅 MDC - 업무 구분 prefix 추출
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CorrelationIdValidator {

    /**
     * UUID v7 형식 정규식
     * - 버전: 7 (타임스탬프 포함)
     * - Variant: RFC 4122 (8, 9, a, b)
     */
    private static final Pattern UUID_V7_PATTERN = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"
    );

    /**
     * Correlation ID 전체 형식 정규식
     * - PREFIX(3자 영문 대문자) + _ + UUID v7
     */
    private static final Pattern CORRELATION_ID_PATTERN = Pattern.compile(
            "^[A-Z]{3}_[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"
    );

    /**
     * 주문 플로우 프리픽스
     */
    public static final String ORDER_PREFIX = "ORD";

    /**
     * Correlation ID 형식 검증
     *
     * @param correlationId 검증할 Correlation ID
     * @return 유효한 형식이면 true
     */
    public static boolean isValid(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            return false;
        }
        return CORRELATION_ID_PATTERN.matcher(correlationId).matches();
    }

    /**
     * 주문 플로우용 Correlation ID 검증
     * - ORD_ 프리픽스 확인
     *
     * @param correlationId 검증할 Correlation ID
     * @return ORD 프리픽스이고 유효한 형식이면 true
     */
    public static boolean isValidOrderCorrelationId(String correlationId) {
        return isValid(correlationId) && correlationId.startsWith(ORDER_PREFIX + "_");
    }

    /**
     * Correlation ID에서 프리픽스 추출
     *
     * @param correlationId Correlation ID
     * @return 프리픽스 (예: ORD)
     * @throws IllegalArgumentException 유효하지 않은 형식인 경우
     */
    public static String extractPrefix(String correlationId) {
        if (!isValid(correlationId)) {
            log.error("Invalid correlationId format: {}", correlationId);
            throw new IllegalArgumentException("유효하지 않은 Correlation ID 형식입니다: " + correlationId);
        }
        return correlationId.substring(0, 3);
    }

    /**
     * Correlation ID에서 UUID 부분 추출
     *
     * @param correlationId Correlation ID
     * @return UUID v7 문자열
     * @throws IllegalArgumentException 유효하지 않은 형식인 경우
     */
    public static String extractUuid(String correlationId) {
        if (!isValid(correlationId)) {
            log.error("Invalid correlationId format: {}", correlationId);
            throw new IllegalArgumentException("유효하지 않은 Correlation ID 형식입니다: " + correlationId);
        }
        return correlationId.substring(4); // "ORD_" 이후 부분
    }

    /**
     * UUID v7 형식 검증
     *
     * @param uuid 검증할 UUID 문자열
     * @return UUID v7 형식이면 true
     */
    public static boolean isValidUuidV7(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return false;
        }
        return UUID_V7_PATTERN.matcher(uuid).matches();
    }

    /**
     * Correlation ID 검증 (예외 발생)
     * - 유효하지 않은 형식이면 IllegalArgumentException 발생
     * - Service 레이어에서 간결하게 사용 가능
     *
     * @param correlationId 검증할 Correlation ID
     * @throws IllegalArgumentException 유효하지 않은 형식인 경우
     */
    public static void validate(String correlationId) {
        if (!isValid(correlationId)) {
            log.error("Invalid correlationId format: {}", correlationId);
            throw new IllegalArgumentException(
                    "유효하지 않은 Correlation ID 형식입니다: " + correlationId
            );
        }
    }
}