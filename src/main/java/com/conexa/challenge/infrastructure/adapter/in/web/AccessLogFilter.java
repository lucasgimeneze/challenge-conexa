package com.conexa.challenge.infrastructure.adapter.in.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Una línea por request, así todo correlationId lleva al menos a un log (también los 401 y 429, porque corre
 * antes que Spring Security). Nunca registra body ni headers. El health check se omite: la plataforma lo
 * consulta cada pocos segundos.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class AccessLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AccessLogFilter.class);

    private static final String HEALTH_PATH = "/actuator/health";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return HEALTH_PATH.equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long start = System.nanoTime();
        int status = HttpStatus.INTERNAL_SERVER_ERROR.value();
        try {
            chain.doFilter(request, response);
            status = response.getStatus();
        } finally {
            long millis = (System.nanoTime() - start) / 1_000_000;
            log.info("{} {} {} ({} ms) from {}",
                    request.getMethod(), pathOf(request), status, millis, request.getRemoteAddr());
        }
    }

    private static String pathOf(HttpServletRequest request) {
        String query = request.getQueryString();
        return query == null ? request.getRequestURI() : request.getRequestURI() + "?" + query;
    }
}
