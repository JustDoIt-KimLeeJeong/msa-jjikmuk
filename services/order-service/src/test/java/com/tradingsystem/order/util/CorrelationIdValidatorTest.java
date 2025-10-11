package com.tradingsystem.order.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

/**
 * CorrelationIdValidator 테스트
 */
@DisplayName("Correlation ID 검증 유틸리티 테스트")
class CorrelationIdValidatorTest {

    @Test
    @DisplayName("유효한 주문 Correlation ID - 성공")
    void validOrderCorrelationId() {
        // given
        String correlationId = "ORD_018e8c7a-9c5e-7000-8000-123456789abc";

        // when & then
        assertThat(CorrelationIdValidator.isValid(correlationId)).isTrue();
        assertThat(CorrelationIdValidator.isValidOrderCorrelationId(correlationId)).isTrue();
    }

    @Test
    @DisplayName("잘못된 프리픽스 - 실패")
    void invalidPrefix() {
        // given
        String correlationId = "PAY_018e8c7a-9c5e-7000-8000-123456789abc";

        // when & then
        assertThat(CorrelationIdValidator.isValid(correlationId)).isTrue(); // 형식은 맞음
        assertThat(CorrelationIdValidator.isValidOrderCorrelationId(correlationId)).isFalse(); // ORD 아님
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ORD_018e8c7a-9c5e-6000-8000-123456789abc",  // UUID v6 (버전 7 아님)
            "ORD_018e8c7a-9c5e-4000-8000-123456789abc",  // UUID v4
            "ORD_018e8c7a-9c5e-7000-c000-123456789abc",  // Variant 오류 (c는 불가)
            "ord_018e8c7a-9c5e-7000-8000-123456789abc",  // 소문자 프리픽스
            "ORDR_018e8c7a-9c5e-7000-8000-123456789abc", // 4자 프리픽스
            "OR_018e8c7a-9c5e-7000-8000-123456789abc",   // 2자 프리픽스
            "018e8c7a-9c5e-7000-8000-123456789abc",      // 프리픽스 없음
            "ORD-018e8c7a-9c5e-7000-8000-123456789abc",  // - 구분자
            ""                                           // 빈 문자열
    })
    @DisplayName("잘못된 Correlation ID 형식 - 실패")
    void invalidCorrelationId(String correlationId) {
        // when & then
        assertThat(CorrelationIdValidator.isValid(correlationId)).isFalse();
    }

    @Test
    @DisplayName("null Correlation ID - 실패")
    void nullCorrelationId() {
        // when & then
        assertThat(CorrelationIdValidator.isValid(null)).isFalse();
        assertThat(CorrelationIdValidator.isValidOrderCorrelationId(null)).isFalse();
    }

    @Test
    @DisplayName("프리픽스 추출 - 성공")
    void extractPrefix() {
        // given
        String correlationId = "ORD_018e8c7a-9c5e-7000-8000-123456789abc";

        // when
        String prefix = CorrelationIdValidator.extractPrefix(correlationId);

        // then
        assertThat(prefix).isEqualTo("ORD");
    }

    @Test
    @DisplayName("UUID 추출 - 성공")
    void extractUuid() {
        // given
        String correlationId = "ORD_018e8c7a-9c5e-7000-8000-123456789abc";

        // when
        String uuid = CorrelationIdValidator.extractUuid(correlationId);

        // then
        assertThat(uuid).isEqualTo("018e8c7a-9c5e-7000-8000-123456789abc");
        assertThat(CorrelationIdValidator.isValidUuidV7(uuid)).isTrue();
    }

    @Test
    @DisplayName("잘못된 형식에서 추출 시도 - 예외 발생")
    void extractFromInvalidFormat() {
        // given
        String invalidCorrelationId = "INVALID_ID";

        // when & then
        assertThatThrownBy(() -> CorrelationIdValidator.extractPrefix(invalidCorrelationId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유효하지 않은 Correlation ID 형식");

        assertThatThrownBy(() -> CorrelationIdValidator.extractUuid(invalidCorrelationId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유효하지 않은 Correlation ID 형식");
    }

    @Test
    @DisplayName("UUID v7 형식 검증")
    void validateUuidV7() {
        // given
        String validUuidV7 = "018e8c7a-9c5e-7000-8000-123456789abc";
        String invalidUuidV4 = "123e4567-e89b-42d3-a456-426614174000"; // v4

        // when & then
        assertThat(CorrelationIdValidator.isValidUuidV7(validUuidV7)).isTrue();
        assertThat(CorrelationIdValidator.isValidUuidV7(invalidUuidV4)).isFalse();
    }
}