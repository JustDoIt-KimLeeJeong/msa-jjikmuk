package com.jjikmuk.execution_service.infrastructure.persistence.repository;

import com.jjikmuk.execution_service.infrastructure.persistence.entity.TradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TradeJpaRepository extends JpaRepository<TradeEntity, Long> {
    Optional<TradeEntity> findByOrderId(String value);
}
