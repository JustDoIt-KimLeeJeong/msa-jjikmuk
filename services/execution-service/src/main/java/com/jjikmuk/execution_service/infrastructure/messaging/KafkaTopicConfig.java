package com.jjikmuk.execution_service.infrastructure.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@Profile({"local", "dev"})
public class KafkaTopicConfig {

    @Value("${app.kafka.partitions.order-events:30}")
    private int orderEventsPartitions;

    @Value("${app.kafka.partitions.market-data-ticks:30}")
    private int marketDataPartitions;

    @Value("${app.kafka.replication-factor:1}") // 로컬/개발 기본 1
    private short replicationFactor;

    /**
     * 주문 이벤트 토픽 생성 설정
     * - name: order-events
     * - partitions: 주문 트래픽 처리량에 따라 조정 (default 30)
     * - replicas: 클러스터 복제본 개수 (개발=1, 운영=3 권장)
     * - cleanup.policy: delete (만료되면 삭제)
     * - retention.ms: 7일 (밀리초 단위)
     */
    @Bean
    public NewTopic orderEventTopic() {
        return TopicBuilder.name("order-events")
                .partitions(orderEventsPartitions)
                .replicas(replicationFactor)
                // 필요시 보관정책/기간
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .config(TopicConfig.RETENTION_MS_CONFIG, "604800000") // 7일
                .build();
    }

    /**
     * 마켓데이터 틱 토픽 생성 설정
     * - name: market-data-ticks
     * - partitions: 심볼별로 파티션을 늘리면 병렬처리 ↑
     * - retention.ms: 1일 (틱 데이터는 길게 보관할 필요 없음)
     */
    @Bean
    public NewTopic marketDataTicksTopic() {
        return TopicBuilder.name("market-data-ticks")
                .partitions(marketDataPartitions)
                .replicas(replicationFactor)
                .config(TopicConfig.RETENTION_MS_CONFIG, "86400000") // 1일
                .build();
    }
}
