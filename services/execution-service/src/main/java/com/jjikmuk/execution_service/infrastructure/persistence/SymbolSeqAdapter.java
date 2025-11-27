package com.jjikmuk.execution_service.infrastructure.persistence;

import com.jjikmuk.execution_service.domain.model.value.Symbol;
import com.jjikmuk.execution_service.domain.port.SymbolSeqPort;
import com.jjikmuk.execution_service.infrastructure.persistence.entity.SymbolSeqEntity;
import com.jjikmuk.execution_service.infrastructure.persistence.repository.SymbolSeqJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SymbolSeqAdapter implements SymbolSeqPort {

    private final SymbolSeqJpaRepository repository;

    /**
     * 주어진 종목(Symbol)에 대한 다음 도착 순번을 생성 및 반환합니다.
     *
     * Propagation.REQUIRES_NEW를 통해 별도의 트랜잭션에서 수행되므로,
     * 호출한 상위 트랜잭션의 롤백 여부와 관계없이 시퀀스 값은 안전하게 증가합니다.
     *
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long nextArrivalSeq(Symbol symbol) {
        // 1. 심볼에 해당하는 시퀀스 엔티티를 비관적 락을 걸어 조회합니다.
        SymbolSeqEntity entity = repository.findBySymbolWithLock(symbol.value())
            // 2. 만약 엔티티가 없으면(해당 심볼의 첫 주문이면), 새로 생성합니다.
            .orElseGet(() -> new SymbolSeqEntity(symbol.value()));

        // 3. 시퀀스를 1 증가시킵니다.
        entity.increase();

        // 4. 변경된 엔티티를 저장합니다.
        repository.save(entity);

        // 5. 증가된 시퀀스 번호를 반환합니다.
        return entity.getSequence();
    }
}
