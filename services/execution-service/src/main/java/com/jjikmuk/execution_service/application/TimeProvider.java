package com.jjikmuk.execution_service.application;

import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * TimeProvider
 *
 * 시스템 시각을 반환하는 유틸리티 컴포넌트.
 * 직접 Instant.now()를 호출하지 않고,
 * 이 클래스를 통해 시간을 주입받으면 테스트와 로깅에서 일관성을 유지할 수 있다.
 */
@Component
public class TimeProvider {

    /**
     * 현재 시스템 시각을 반환한다.
     */
    public Instant now() {
        return Instant.now();
    }
}