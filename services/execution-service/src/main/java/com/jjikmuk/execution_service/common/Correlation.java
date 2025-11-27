package com.jjikmuk.execution_service.common;

public final class Correlation {
    public static final String HEADER = "x-correlation-id"; // Kafka header name
    public static final String MDC_KEY = "correlationId";   // Log MDC key
    private Correlation() {}
}
