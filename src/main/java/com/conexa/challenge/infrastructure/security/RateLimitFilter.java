package com.conexa.challenge.infrastructure.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.internal.AtomicRateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Va después de validar el JWT: el catálogo se limita por usuario y una request sin token responde 401
 * sin consumir cupo. Auth se limita por IP porque todavía no hay usuario.
 */
class RateLimitFilter extends OncePerRequestFilter {

    private static final Duration IDLE_LIMITER_EXPIRATION = Duration.ofHours(1);
    private static final int MAX_TRACKED_CLIENTS = 10_000;

    private final RateLimitProperties properties;
    private final HandlerExceptionResolver exceptionResolver;
    private final Cache<String, AtomicRateLimiter> limiters = Caffeine.newBuilder()
            .expireAfterAccess(IDLE_LIMITER_EXPIRATION)
            .maximumSize(MAX_TRACKED_CLIENTS)
            .build();

    RateLimitFilter(RateLimitProperties properties, HandlerExceptionResolver exceptionResolver) {
        this.properties = properties;
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Optional<AtomicRateLimiter> limiter = limiterFor(request);
        if (limiter.isPresent() && !limiter.get().acquirePermission()) {
            Duration retryAfter = Duration.ofNanos(limiter.get().getDetailedMetrics().getNanosToWait());
            exceptionResolver.resolveException(request, response, null, new RateLimitExceededException(retryAfter));
            return;
        }
        chain.doFilter(request, response);
    }

    private Optional<AtomicRateLimiter> limiterFor(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.startsWith("/auth/")) {
            return Optional.of(limiter("auth:" + request.getRemoteAddr(), properties.auth()));
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (path.startsWith("/api/") && authentication instanceof JwtAuthenticationToken) {
            return Optional.of(limiter("api:" + authentication.getName(), properties.api()));
        }
        return Optional.empty();
    }

    private AtomicRateLimiter limiter(String key, RateLimitProperties.Limit limit) {
        return limiters.get(key, name -> new AtomicRateLimiter(name, RateLimiterConfig.custom()
                .limitForPeriod(limit.limit())
                .limitRefreshPeriod(limit.period())
                .timeoutDuration(Duration.ZERO)
                .build()));
    }
}
