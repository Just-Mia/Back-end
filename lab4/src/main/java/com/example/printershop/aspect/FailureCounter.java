package com.example.printershop.aspect;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;


@Component
public class FailureCounter {

    private final Map<String, AtomicLong> failures = new ConcurrentHashMap<>();

    public long increment(String method) {
        return failures.computeIfAbsent(method, k -> new AtomicLong()).incrementAndGet();
    }

    public long get(String method) {
        AtomicLong value = failures.get(method);
        return value == null ? 0 : value.get();
    }
}
