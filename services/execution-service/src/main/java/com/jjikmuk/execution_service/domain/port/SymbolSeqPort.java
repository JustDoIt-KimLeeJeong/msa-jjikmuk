package com.jjikmuk.execution_service.domain.port;

import com.jjikmuk.execution_service.domain.model.value.Symbol;

public interface SymbolSeqPort {

    long nextArrivalSeq(Symbol symbol);

}
