package com.jjikmuk.execution_service.domain.service;

import com.jjikmuk.execution_service.domain.model.Fill;
import com.jjikmuk.execution_service.domain.model.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class MatchingEngine {
    // 매칭 규칙 (Phase-1: 시장가 = 최근가 1회 체결)
    public List<Fill> marketAgainstLastPrice(Order order, BigDecimal lastPrice, Instant now) {
        //최근가 없으면 미체결

        //로직

        return List.of(new Fill(new BigDecimal(0),0,now));
    }

    // Phase-2: limitAgainstOrderBook(...) 추가 예정.
}
