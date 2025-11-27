package com.tradingsystem.order.dto.request;

import com.tradingsystem.order.domain.OrderSide;
import com.tradingsystem.order.domain.OrderStatus;
import com.tradingsystem.order.domain.OrderType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * 주문 생성 요청 DTO
 * - 시장가/지정가 매수/매도 주문 생성
 * - Validation 규칙: 시장가는 price 불필요, 지정가는 price 필수
 * - 한국 주식 시장 규칙 반영 (종목코드 6자리, 가격 정수)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@ToString
public class CreateOrderRequest {

    /**
     * 분산 추적 ID(Correlation ID)
     * - BFF에서 HTTP Header로 전달(X-Correlation-Id)
     * - 형식: PREFIX_UUID_v7 (예: ORD_018e8c7a-9c5e-7000-8000-123456789abc)
     * - 전체 요청 플로우 추적용
     * - 로그/이벤트에 포함하여 디버깅 용이
     */
    @NotBlank(message = "Correlation ID는 필수입니다.")
    @Pattern(
            regexp = "^[A-Z]{3}_[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$",
            message = "Correlation ID 형식이 올바르지 않습니다. (예: ORD_018e8c7a-9c5e-7000-8000-123456789abc)"
    )
    private String correlationId;

    /**
     * 클라이언트 주문 ID (멱등성 보장용)
     * - 프론트엔드에서 생성 (숫자, 타임스탬프 등)
     *      * - 중복 주문 방지 (userId + clientOrderId 조합)
     *      * - 예: "1234567890", "20250111-001", "order-123"
     *      */
    @NotBlank(message = "클라이언트 주문 ID는 필수입니다")
    @Size(min = 1, max = 50, message = "클라이언트 주문 ID는 1~50자여야 합니다")
    @Pattern(
             regexp ="^[a-zA-Z0-9-_]+$",
            message ="클라이언트 주문 ID는 영문, 숫자, 하이픈(-), 언더스코어(_)만 가능합니다"
    )
    private String clientOrderId;

    /**
     * 종목 코드 (한국 주식 6자리 숫자)
     * - 예: 005930 (삼성전자), 035720 (카카오)
     * - 앞자리 0 포함 필수
     */
    @NotBlank(message = "종목 코드는 필수입니다")
    @Pattern(regexp = "^[0-9]{6}$", message = "종목 코드는 6자리 숫자여야 합니다")
    private String symbol;

    /**
     * 매수/매도 구분
     */
    @NotNull(message = "매수/매도 구분은 필수입니다")
    private OrderSide side;

    /**
     * 주문 유형(시장가/지정가)
     */
    @NotNull(message = "주문 유형은 필수입니다.")
    private OrderType type;

    /**
     * 지정가 (지정가 주문인 경우 필수, 한국 주식은 정수만)
     * - 시장가 주문 : null
     * - 지정가 주문 : 양수 정수 필수
     * - 호가 단위는 별도 검증 필요 (1원, 5원, 10원, 50원, 100원)
     */
    @Min(value = 1, message = "가격은 1원 이상이어야 합니다")
    @Max(value = 10000000, message = "가격은 1,000만원을 초과할 수 없습니다")
    private Integer price;

    /**
     * 주문 수량
     */
    @NotNull(message = "주문 수량은 필수입니다")
    @Min(value = 1, message = "주문 수량은 최소 1주 이상이어야 합니다")
    @Max(value = 1000000, message = "주문 수량은 최대 1,000,000주를 초과할 수 없습니다")
    private Integer quantity;

    // === 비즈니스 검증 메서드 ===

    /**
     * 시장가 주문 검증
     * - 시장가는 price가 null이어야 함
     */
    public boolean isValidMarketOrder() {
        return type == OrderType.MARKET && price == null;
    }

    /**
     * 지정가 주문 검증
     * - 지정가는 price가 필수
     */
    public boolean isValidLimitOrder() {
        return type == OrderType.LIMIT && price != null && price > 0;
    }

    /**
     * 전체 검증 (컨트롤러 레벨 추가 검증용)
     */
    public void validateOrderTypeAndPrice() {
        if (type == OrderType.MARKET && price != null) {
            throw new IllegalArgumentException("시장가 주문은 가격을 지정할 수 없습니다");
        }

        if (type == OrderType.LIMIT && (price == null || price <= 0)) {
            throw new IllegalArgumentException("지정가 주문은 양수 가격이 필수입니다");
        }
    }


}
