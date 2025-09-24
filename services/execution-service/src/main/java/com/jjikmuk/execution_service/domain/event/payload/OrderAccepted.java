package com.jjikmuk.execution_service.domain.event.payload;

import com.jjikmuk.execution_service.domain.model.Order;

public record OrderAccepted (
        String eventId,
        String orderId,
        String userId,
        String symbol,
        String side,
        String type,
        String tif,
        String price, // LIMIT 때만 사용
        Long quantity,
        Boolean isMarketHours


){
    public Order.Side sideEnum() { return Order.Side.valueOf(side); }
    public Order.Type typeEnum() { return Order.Type.valueOf(type); }
    public Order.Tif  tifEnum()  { return Order.Tif.valueOf(tif); }
}
