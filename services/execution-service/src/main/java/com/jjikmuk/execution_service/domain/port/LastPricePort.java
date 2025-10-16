package com.jjikmuk.execution_service.domain.port;

import java.util.Optional;

public interface LastPricePort {
    Optional<Long> getLastPrice(String symbol);
    void updatePrice(String symbol, long price);

    // 멀티 인스턴스 : 서버 여러개 띄워서 로드밸런싱할 경우
    // - InMemory -> redis 같은거 써야 함.
}
