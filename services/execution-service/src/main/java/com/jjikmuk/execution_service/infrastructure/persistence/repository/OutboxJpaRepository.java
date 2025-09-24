package com.jjikmuk.execution_service.infrastructure.persistence.repository;

import com.jjikmuk.execution_service.infrastructure.persistence.entity.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OutboxJpaRepository extends JpaRepository<OutboxEventEntity, UUID> {
}
