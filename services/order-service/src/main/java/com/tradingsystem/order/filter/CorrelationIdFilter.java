package com.tradingsystem.order.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * BFF에서 전달되는 필수 헤더를 검증하고 MDC에 저장하는 필터
 */
@Slf4j
@Component
@Order(1)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String CORRELATION_ID_KEY = "correlationId";
    private static final String USER_ID_KEY = "userId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        try {
            // Health Check는 헤더 검증 제외
            if (isHealthCheckEndpoint(requestURI)) {
                filterChain.doFilter(request, response);
                return;
            }

            // Correlation ID 검증
            String correlationId = request.getHeader(CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.isBlank()) {
                log.error("Missing {} header - URI: {}", CORRELATION_ID_HEADER, requestURI);
                sendErrorResponse(response, CORRELATION_ID_HEADER + " header is required");
                return;
            }

            // User ID 검증
            String userId = request.getHeader(USER_ID_HEADER);
            if (userId == null || userId.isBlank()) {
                log.error("Missing {} header - URI: {}, CorrelationId: {}",
                        USER_ID_HEADER, requestURI, correlationId);
                sendErrorResponse(response, USER_ID_HEADER + " header is required");
                return;
            }

            // MDC에 저장
            MDC.put(CORRELATION_ID_KEY, correlationId);
            MDC.put(USER_ID_KEY, userId);

            log.info("Request started - URI: {}, CorrelationId: {}, UserId: {}",
                    requestURI, correlationId, userId);

            filterChain.doFilter(request, response);

            log.info("Request completed - CorrelationId: {}", correlationId);

        } finally {
            MDC.clear();
        }
    }

    private boolean isHealthCheckEndpoint(String uri) {
        return uri.startsWith("/actuator/health")
                || uri.equals("/health")
                || uri.equals("/actuator/info");
    }

    private void sendErrorResponse(HttpServletResponse response, String message)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json;charset=UTF-8");

        String json = String.format(
                "{\"timestamp\":\"%s\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"%s\"}",
                LocalDateTime.now(),
                message
        );

        response.getWriter().write(json);
    }
}