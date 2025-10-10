package com.tradingsystem.order.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

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

    @Column(nullable = false, length = 50)
    private String eventId; // UUID(멱등성 보장)

    @Column(nullable = false, length = 50)
    private String eventType;  // OrderPlaced, OrderCancelled, OrderExpired

    @Column(nullable = false)
    private Long aggregateId; // Order ID

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload; // JSON 형태의 이벤트 데이터

    @Column(nullable = false)
    @Builder.Default
    private boolean published = false;

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
