package com.example.printershop.configuration;

import com.example.printershop.service.LoggingAsyncExceptionHandler;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;


@Configuration
@EnableAsync(proxyTargetClass = true)
public class AsyncConfig implements AsyncConfigurer {

    public static final String EXECUTOR = "shopTaskExecutor";

    private final LoggingAsyncExceptionHandler exceptionHandler = new LoggingAsyncExceptionHandler();

    @Bean(name = EXECUTOR)
    public ThreadPoolTaskExecutor shopTaskExecutor(
            @Value("${shop.async.core-pool-size:4}") int corePoolSize,
            @Value("${shop.async.max-pool-size:8}") int maxPoolSize,
            @Value("${shop.async.queue-capacity:20}") int queueCapacity,
            @Value("${shop.async.thread-name-prefix:shop-async-}") String threadNamePrefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);

        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        return executor;
    }

    @Bean
    public LoggingAsyncExceptionHandler asyncExceptionHandler() {
        return exceptionHandler;
    }


    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return exceptionHandler;
    }
}
