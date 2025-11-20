package com.tradingsystem.order.repository;

import com.tradingsystem.order.domain.OutboxEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    // ========== 핵심 메서드 ==========

    /**
     * eventId 기반 중복 체크 (멱등성 보장)
     * - EventPublisher에서 이벤트 발행 전 중복 확인
     * - UNIQUE 제약조건으로 인덱스 최적화
     */
    Optional<OutboxEvent> findByEventId(String eventId);

    // ========== 모니터링/디버깅용 ==========

    /**
     * 미발행 이벤트 조회 (모니터링용)
     * - CDC 장애 시 수동 점검
     * - 대시보드에서 "발행 대기 이벤트" 표시
     *
     * 주의: CDC 패턴에서는 WAL에서 자동 복구되므로 재발행 로직에는 미사용
     */
    Page<OutboxEvent> findByPublishedFalse(Pageable pageable);

    /**
     * CorrelationId로 이벤트 체인 조회 (디버깅용)
     * - 특정 요청의 전체 이벤트 히스토리 추적
     * - 고객 문의 대응 시 사용
     *
     * 예시: correlationId="corr-abc-123"인 모든 이벤트 조회
     */
    List<OutboxEvent> findByCorrelationIdOrderByCreatedAtAsc(String correlationId);

    // ========== 배치 정리용 (스케줄러) ==========

    /**
     * 오래된 발행 완료 이벤트 조회 (정리 배치용)
     * - published = true
     * - publishedAt < 기준시간
     *
     * 사용 예시:
     * LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
     * List<OutboxEvent> oldEvents = repository.findOldPublishedEvents(cutoff);
     */
    @Query("SELECT oe FROM OutboxEvent oe WHERE oe.published = true " +
            "AND oe.publishedAt < :cutoffTime " +
            "ORDER BY oe.publishedAt ASC")
    List<OutboxEvent> findOldPublishedEvents(
            @Param("cutoffTime") LocalDateTime cutoffTime,
            Pageable pageable);

    /**
     * 오래된 발행 완료 이벤트 일괄 삭제
     * - Bulk Delete로 성능 최적화
     * - 트랜잭션 내에서 실행 필요 (@Transactional)
     *
     * @return 삭제된 행 수
     */
    @Modifying
    @Query("DELETE FROM OutboxEvent oe WHERE oe.published = true " +
            "AND oe.publishedAt < :cutoffTime")
    int deleteOldPublishedEvents(@Param("cutoffTime") LocalDateTime cutoffTime);
}