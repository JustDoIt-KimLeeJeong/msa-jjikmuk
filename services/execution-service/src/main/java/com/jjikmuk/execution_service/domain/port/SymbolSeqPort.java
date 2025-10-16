package com.jjikmuk.execution_service.domain.port;

import com.jjikmuk.execution_service.domain.model.value.Symbol;

public interface SymbolSeqPort {
    /**
     * 지정된 종목(Symbol)에 대한 다음 도착 순번을 반환합니다.
     *
     * @param symbol 순번을 생성할 종목 코드
     * @return 다음 도착 순번 (long 타입)
     */
    long nextArrivalSeq(Symbol symbol);

}
