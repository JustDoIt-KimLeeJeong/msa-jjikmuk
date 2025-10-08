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
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@EntityListeners(AuditingEntityListener.class)
public class OutboxEvent {

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

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime publishedAt;

    // === 비즈니스 메서드 ===
    /**
     * 이벤트 발행 완료 처리
     *  @param publishedTime 이벤트가 실제로 발행 완료된 시간
     */
    public void markAsPublished(LocalDateTime publishedTime) {
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
