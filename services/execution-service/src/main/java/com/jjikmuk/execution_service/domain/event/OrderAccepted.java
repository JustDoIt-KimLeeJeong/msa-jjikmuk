package com.jjikmuk.execution_service.domain.event;

import com.jjikmuk.execution_service.domain.model.Order;

public record OrderAccepted (
    String orderId, String symbol, String side, String type, Long qty,
    String tif, String price // LIMIT 때만 사용
){
    public Order.Side sideEnum() { return Order.Side.valueOf(side); }
    public Order.Type typeEnum() { return Order.Type.valueOf(type); }
    public Order.Tif  tifEnum()  { return Order.Tif.valueOf(tif); }
}
