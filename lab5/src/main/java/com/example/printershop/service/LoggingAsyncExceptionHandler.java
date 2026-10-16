package com.example.printershop.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicLong;


@Slf4j
public class LoggingAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    private final AtomicLong failures = new AtomicLong();

    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {
        long total = failures.incrementAndGet();

        log.error("[Async] uncaught failure in {}.{}() (args: {}, total async failures: {})",
                method.getDeclaringClass().getSimpleName(), method.getName(), params.length, total, ex);
    }

    public long getFailureCount() {
        return failures.get();
    }
}
