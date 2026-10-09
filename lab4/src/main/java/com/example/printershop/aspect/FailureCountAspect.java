package com.example.printershop.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;


@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class FailureCountAspect {

    private final FailureCounter counter;

    public FailureCountAspect(FailureCounter counter) {
        this.counter = counter;
    }

    @AfterThrowing(pointcut = "@annotation(com.example.printershop.domain.annotation.CountFailures)", throwing = "ex")
    public void countFailure(JoinPoint joinPoint, Throwable ex) {
        String method = joinPoint.getSignature().toShortString();
        long total = counter.increment(method);
        log.warn("[Failures] {} failed with {} (total failures: {})", method, ex.getClass().getSimpleName(), total);
    }
}
