package com.jjikmuk.execution_service.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

//설정 바인딩
@ConfigurationProperties(prefix = "app")
public record AppProperties(boolean limitEnabled) {
}
