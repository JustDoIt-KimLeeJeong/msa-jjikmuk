package com.jjikmuk.execution_service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjikmuk.execution_service.domain.event.DomainEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

@Configuration
public class KafkaConfig {

    // Kafka 브로커 주소 (환경변수 없으면 기본값 "kafka:9092")
    private static final String BOOTSTRAP =
            System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "kafka:9092");

    /**
     * DomainEvent 전송용 ProducerFactory
     * - Key: String
     * - Value: DomainEvent (JsonSerializer)
     */
    @Bean
    public ProducerFactory<String, DomainEvent> producerFactory() {
        return new DefaultKafkaProducerFactory<>(
                Map.of(
                        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP,
                        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class
                )
        );
    }

    /**
     * KafkaTemplate
     * - 실제로 Kafka 토픽에 메시지를 publish 하는 객체
     * - 주입 받아서 사용
     */
    @Bean
    public KafkaTemplate<String, DomainEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }


    /**
     * DomainEvent 수신용 ConsumerFactory
     * - Key: String
     * - Value: DomainEvent (JsonDeserializer)
     * - group.id: execution-service
     * 참고:
     * - addTrustedPackages("*") → 모든 패키지 허용 (운영에서는 특정 패키지만 허용 권장)
     * - AUTO_OFFSET_RESET = earliest → 오프셋 없을 시 가장 처음부터 읽기
     */
    @Bean
    public ConsumerFactory<String, DomainEvent> domainEventConsumerFactory() {
        JsonDeserializer<DomainEvent> jd = new JsonDeserializer<>(DomainEvent.class);
        jd.addTrustedPackages("*"); // TODO: 운영 환경에서는 보안 위해 제한 필요

        return new DefaultKafkaConsumerFactory<>(
                Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP,
                        ConsumerConfig.GROUP_ID_CONFIG, "execution-service",
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"
                ),
                new StringDeserializer(), jd
        );
    }

    /**
     * DomainEvent 컨슈머 컨테이너 팩토리
     * - @KafkaListener 에서 containerFactory="domainEventKafkaListenerContainerFactory" 로 지정 가능
     * - concurrency: 파티션 수에 맞춰 병렬 컨슈머 생성
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DomainEvent> domainEventKafkaListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, DomainEvent>();
        factory.setConsumerFactory(domainEventConsumerFactory());
        factory.setConcurrency(3); // 파티션 수와 맞춰야 성능/순서 보장
        return factory;
    }

    /**
     * Market Data Tick 수신용 ConsumerFactory
     * - Key: String
     * - Value: String (raw JSON or plain text tick)
     * - group.id: market-data-service
     */
    @Bean
    public ConsumerFactory<String, String> tickConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(
                Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP,
                        ConsumerConfig.GROUP_ID_CONFIG, "market-data-service",
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"
                ),
                new StringDeserializer(), new StringDeserializer()
        );
    }

    /**
     * Tick 컨슈머 컨테이너 팩토리
     * - @KafkaListener 에서 containerFactory="tickKafkaListenerContainerFactory" 로 지정
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> tickKafkaListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
        factory.setConsumerFactory(tickConsumerFactory());
        return factory;
    }

    /**
     * Jackson ObjectMapper
     * - Json 직렬화/역직렬화 편의를 위해 빈 등록
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

}