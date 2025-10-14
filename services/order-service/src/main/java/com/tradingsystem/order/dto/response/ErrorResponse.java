package com.tradingsystem.order.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 에러 응답 DTO
 * - 일관된 에러 응답 포맷 제공
 * - 프론트엔드에서 파싱하기 쉬운 구조
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@ToString
public class ErrorResponse {

    /**
     * 에러 코드
     * - DUPLICATE_ORDER: 중복 주문
     * - ORDER_NOT_FOUND: 주문 미발견
     * - INVALID_REQUEST: 잘못된 요청
     * - INVALID_STATE: 잘못된 상태
     * - VALIDATION_ERROR: Validation 오류
     * - INTERNAL_ERROR: 서버 내부 오류
     */
    private String code;

    /**
     * 에러 메시지 (사용자 친화적)
     */
    private String message;

    /**
     * 에러 발생 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * ErrorResponse 생성 팩토리 메서드
     */
    public static ErrorResponse of(String code, String message) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}