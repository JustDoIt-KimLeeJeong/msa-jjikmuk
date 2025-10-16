package com.jjikmuk.execution_service.infrastructure.persistence.repository;

import com.jjikmuk.execution_service.infrastructure.persistence.entity.FillEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FillJpaRepository extends JpaRepository<FillEntity, Long> {
}
