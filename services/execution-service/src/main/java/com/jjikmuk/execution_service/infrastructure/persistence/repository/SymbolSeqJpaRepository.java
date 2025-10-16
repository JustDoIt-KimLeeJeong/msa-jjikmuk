package com.jjikmuk.execution_service.infrastructure.persistence.repository;

import com.jjikmuk.execution_service.infrastructure.persistence.entity.SymbolSeqEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SymbolSeqJpaRepository extends JpaRepository<SymbolSeqEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)   // 비관적 락
    @Query("SELECT s FROM SymbolSeqEntity s WHERE s.symbol = :symbol")
    Optional<SymbolSeqEntity> findBySymbolWithLock(@Param("symbol") String symbol);

}
