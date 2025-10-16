package com.jjikmuk.execution_service.infrastructure.cache;

import com.jjikmuk.execution_service.domain.port.LastPricePort;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryLastPriceAdapter implements LastPricePort {

    private final Map<String, Long> cache = new ConcurrentHashMap<>();


    @Override
    public Optional<Long> getLastPrice(String symbol) {
        return Optional.ofNullable(cache.get(symbol));
    }

    @Override
    public void updatePrice(String symbol, long price) {
        cache.put(symbol,price);

    }
}
