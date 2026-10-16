package com.example.printershop.async;

import com.example.printershop.async.AsyncTestSupport.ControllableMailGateway;
import com.example.printershop.configuration.AsyncConfig;
import com.example.printershop.service.EmailService;
import com.example.printershop.service.LoggingAsyncExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.concurrent.ThreadPoolExecutor;

import static com.example.printershop.async.AsyncTestSupport.await;
import static com.example.printershop.async.AsyncTestSupport.order;
import static com.example.printershop.async.AsyncTestSupport.waitUntil;
import static org.assertj.core.api.Assertions.assertThat;



@SpringJUnitConfig(AsyncExecutorTest.Config.class)
class AsyncExecutorTest {

    @Configuration
    @Import({AsyncConfig.class, EmailService.class})
    static class Config {
        @Bean
        ControllableMailGateway mailGateway() {
            return new ControllableMailGateway();
        }
    }

    @Autowired
    private EmailService emailService;
    @Autowired
    private ControllableMailGateway gateway;
    @Autowired
    @Qualifier(AsyncConfig.EXECUTOR)
    private ThreadPoolTaskExecutor executor;
    @Autowired
    private LoggingAsyncExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        gateway.reset();
    }

    @Test
    @DisplayName("виконавець має явно задані розміри пулу, чергу, префікс і політику відхилення")
    void executorIsExplicitlyConfigured() {
        assertThat(executor.getCorePoolSize()).isEqualTo(4);
        assertThat(executor.getMaxPoolSize()).isEqualTo(8);
        assertThat(executor.getThreadNamePrefix()).isEqualTo("shop-async-");
        ThreadPoolExecutor pool = executor.getThreadPoolExecutor();
        assertThat(pool.getQueue().remainingCapacity()).isEqualTo(20);
        assertThat(pool.getRejectedExecutionHandler()).isInstanceOf(ThreadPoolExecutor.AbortPolicy.class);
    }

    @Test
    @DisplayName("@Async-метод працює в потоці нашого пулу, а не в main / task- / SimpleAsyncTaskExecutor")
    void asyncMethodRunsOnOwnExecutor() throws Exception {
        emailService.sendOrderConfirmation(order());

        assertThat(await(gateway.finished)).isTrue();
        assertThat(gateway.threadName)
                .startsWith("shop-async-")
                .doesNotStartWith("task-")
                .doesNotContain("SimpleAsyncTaskExecutor")
                .isNotEqualTo(Thread.currentThread().getName());
    }

    @Test
    @DisplayName("fire-and-forget: виклик повертає керування ДО завершення повільного методу")
    void callerIsNotBlocked() throws Exception {
        gateway.release = new java.util.concurrent.CountDownLatch(1);

        emailService.sendOrderConfirmation(order());


        assertThat(await(gateway.started)).isTrue();
        assertThat(gateway.finished.getCount()).isEqualTo(1);

        gateway.release.countDown();
        assertThat(await(gateway.finished)).isTrue();
    }

    @Test
    @DisplayName("@Async void кидає виняток -> AsyncUncaughtExceptionHandler фіксує помилку")
    void voidFailureIsRecordedByHandler() throws Exception {
        long before = exceptionHandler.getFailureCount();
        gateway.fail = true;

        emailService.sendOrderConfirmation(order());

        waitUntil(() -> exceptionHandler.getFailureCount() > before);
        assertThat(exceptionHandler.getFailureCount()).isEqualTo(before + 1);
    }
}
