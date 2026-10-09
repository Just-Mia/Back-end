package com.example.printershop.aspect;

import com.example.printershop.domain.annotation.RateLimited;
import com.example.printershop.exception.TooManyRequestsException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class RateLimitAspect {

    private final Clock clock;
    private final Map<String, Deque<Long>> calls = new ConcurrentHashMap<>();

    public RateLimitAspect(Clock clock) {
        this.clock = clock;
    }

    @Around("@annotation(rateLimited)")
    public Object limit(ProceedingJoinPoint joinPoint, RateLimited rateLimited) throws Throwable {
        String method = joinPoint.getSignature().toShortString();
        String key = method + "|" + currentUser();
        long now = clock.millis();
        long windowMs = rateLimited.perSeconds() * 1000L;

        Deque<Long> window = calls.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && now - window.peekFirst() >= windowMs) {
                window.pollFirst();
            }
            if (window.size() >= rateLimited.maxCalls()) {
                long oldest = window.isEmpty() ? now : window.peekFirst();
                long retryAfterSec = Math.max(1, (windowMs - (now - oldest) + 999) / 1000);
                log.warn("[RateLimit] BLOCKED {} (limit {} per {}s)", key, rateLimited.maxCalls(), rateLimited.perSeconds());
                throw new TooManyRequestsException(
                        "Too many requests, retry in " + retryAfterSec + " s");
            }
            window.addLast(now);
        }
        log.info("[RateLimit] allowed {}", key);
        return joinPoint.proceed();
    }

    private String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() ? auth.getName() : "anonymous";
    }
}
