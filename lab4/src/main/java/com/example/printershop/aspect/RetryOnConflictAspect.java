package com.example.printershop.aspect;

import com.example.printershop.domain.annotation.RetryOnConflict;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Component;


@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 3)
public class RetryOnConflictAspect {

    @Around("@annotation(retry)")
    public Object retry(ProceedingJoinPoint joinPoint, RetryOnConflict retry) throws Throwable {
        String method = joinPoint.getSignature().toShortString();
        int maxAttempts = Math.max(1, retry.maxAttempts());

        for (int attempt = 1; ; attempt++) {
            try {
                return joinPoint.proceed();
            } catch (TransientDataAccessException ex) {
                if (attempt >= maxAttempts) {
                    log.error("[Retry] {} failed after {} attempts", method, attempt);
                    throw ex;
                }
                log.warn("[Retry] {} attempt {}/{} failed: {}", method, attempt, maxAttempts, ex.getMessage());
                pause(retry.backoffMs(), ex);
            }
        }
    }

    private void pause(long millis, RuntimeException cause) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw cause;
        }
    }
}
