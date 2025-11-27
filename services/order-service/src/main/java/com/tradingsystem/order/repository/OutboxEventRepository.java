package com.tradingsystem.order.repository;

import com.tradingsystem.order.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    /**
     * eventId 기반 중복 체크(멱등성 보장)
     */
    Optional<OutboxEvent> findByEventId(String eventId);

    /**
     * 미발행 이벤트 조회 (CDC 백업용)
     * - published = false
     * - 생성시간 기준 오래된 순
     */
    List<OutboxEvent> findByPublishedFalseOrderByCreatedAtAsc();

    /**
     * 오래된 발행 완료 이벤트 조회 (정리용)
     * - published = true
     * - 생성시간 < 기준시간
     */
    @Query("SELECT oe FROM OutboxEvent oe WHERE oe.published = true " +
            "AND oe.createdAt < :cutoffTime")
    List<OutboxEvent> findOldPublishedEvents(@Param("cutoffTime") LocalDateTime cutoffTime);

}
