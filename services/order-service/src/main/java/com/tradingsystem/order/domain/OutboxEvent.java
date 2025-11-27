package com.tradingsystem.order.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Outbox Event 엔티티
 * - CDC(Change Data Capture) 패턴으로 이벤트 발행
 * - 트랜잭션 내에서 생성 후 Debezium이 Kafka로 자동 발행
 * - correlationId 포함하여 분산 추적 지원
 */
@Entity
@Table(name = "outbox_events",
        indexes = {
            @Index(name = "idx_published_created_at", columnList = "published, createdAt")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_event_id", columnNames = {"eventId"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class OutboxEvent extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 이벤트 고유 ID (UUID - 멱등성 보장)
     */
    @Column(nullable = false, length = 50)
    private String eventId;

    /**
     * 분산 추적 ID (Correlation ID)
     * - BFF에서 전달받은 요청 추적 ID
     * - 전체 플로우를 하나의 ID로 추적
     * - 로그 집계 및 디버깅에 필수
     */
    @Column(nullable = false, length = 100)
    private String correlationId;

    /**
     * 이벤트 타입
     * - OrderPlaced, OrderCancelled, OrderExpired 등
     */
    @Column(nullable = false, length = 50)
    private String eventType;

    /**
     * 집합 루트 ID (Order ID)
     */
    @Column(nullable = false)
    private Long aggregateId;

    /**
     * 이벤트 페이로드 (JSON 형태)
     * - 이벤트 상세 데이터
     * - Consumer가 파싱하여 사용
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    /**
     * 발행 여부
     * - CDC가 자동으로 발행하므로 실제로는 사용 안 할 수도 있음
     * - 모니터링/추적 목적으로 유지
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean published = false;

    /**
     * 발행 완료 시간
     */
    private LocalDateTime publishedAt;

    // === 비즈니스 메서드 ===
    /**
     * 이벤트 발행 완료 처리
     *  @param publishedTime 이벤트가 실제로 발행 완료된 시간
     */
    public void markAsPublished(LocalDateTime publishedTime) {
        if (this.published) {
            // 이미 발행된 이벤트에 대한 중복 호출 방지
            throw new IllegalStateException("이미 발행된 이벤트입니다. EventId: " + this.eventId);
        }
        this.published =true;
        this.publishedAt = publishedTime;
    }

    /**
     * 발행 대기 중인 이벤트 여부
     */
    public boolean isPending() {
        return !this.published;
    }
}
