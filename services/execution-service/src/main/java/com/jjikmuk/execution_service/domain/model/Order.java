package com.jjikmuk.execution_service.domain.model;

import com.jjikmuk.execution_service.domain.model.value.OrderId;
import com.jjikmuk.execution_service.domain.model.value.Symbol;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
public class Order {
    public enum Side { BUY, SELL }
    public enum Type { MARKET, LIMIT }
    public enum Tif  { GFD, IOC }       // IOC : Immediate Or Cancel 오늘 문 닫을 때까지 유효
                                        // 즉시 체결 가능한 만큼만 하고, 남은 건 자동 취소.
                                        // GFD : Good For Day 하루(장 마감까지) 살아남음

    private final OrderId   orderId;
    private final Symbol    symbol;
    private final Side      side;
    private final Type      type;
    private final BigDecimal price;         // MARKET면 null
    private final long      origQty;
    @Setter private long    leavesQty;      // LeavesQty 잔량
    private final Tif       tif;            // Time in Force 주문이 언제까지 유효한가 정책
    private final long      arrivalSeq;     // 가격 동일 시 시간 우선
    private final Instant   createdAt;

    // ----- 비즈니스 로직 -----//

    public boolean isMarket() { return type == Type.MARKET; }

    /**
     * 매수 희망 가격 >= 매도 희망 가격일 때 거래 발생
     */
    public boolean crosses(BigDecimal bestOppositePrice) {
        // 시장가
        if (type == Type.MARKET) return true;
        if (side == Side.BUY)    return bestOppositePrice.compareTo(price) <= 0;
        else                     return bestOppositePrice.compareTo(price) >= 0;
    }
}
