package com.example.printershop.async;

import com.example.printershop.async.AsyncTestSupport.ControllableMailGateway;
import com.example.printershop.async.AsyncTestSupport.StubPdfRenderer;
import com.example.printershop.async.AsyncTestSupport.StubShippingApi;
import com.example.printershop.configuration.AsyncConfig;
import com.example.printershop.service.EmailService;
import com.example.printershop.service.InvoicePdfService;
import com.example.printershop.service.OrderFulfillmentService;
import com.example.printershop.service.OrderNotificationService;
import com.example.printershop.service.ShippingQuoteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.example.printershop.async.AsyncTestSupport.order;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@SpringJUnitConfig(ExecutorRejectionTest.Config.class)
@TestPropertySource(properties = {
        "shop.async.core-pool-size=1",
        "shop.async.max-pool-size=1",
        "shop.async.queue-capacity=1"
})
class ExecutorRejectionTest {

    @Configuration
    @Import({AsyncConfig.class, EmailService.class, OrderNotificationService.class,
            InvoicePdfService.class, ShippingQuoteService.class, OrderFulfillmentService.class})
    static class Config {
        @Bean
        ControllableMailGateway mailGateway() {
            return new ControllableMailGateway();
        }

        @Bean
        StubPdfRenderer pdfRenderer() {
            return new StubPdfRenderer();
        }

        @Bean
        StubShippingApi shippingApi() {
            return new StubShippingApi();
        }
    }

    @Autowired
    @Qualifier(AsyncConfig.EXECUTOR)
    private ThreadPoolTaskExecutor executor;
    @Autowired
    private OrderFulfillmentService fulfillmentService;
    @Autowired
    private OrderNotificationService notificationService;

    @Test
    @DisplayName("пул і черга заповнені -> TaskRejectedException; лист пропускається без помилки")
    void overflowIsRejectedAndHandledExplicitly() {
        CountDownLatch release = new CountDownLatch(1);
        Runnable blocker = () -> {
            try {
                release.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
        try {
            executor.execute(blocker);
            executor.execute(blocker);


            assertThatThrownBy(() -> fulfillmentService.prepare(order()))
                    .isInstanceOf(TaskRejectedException.class);


            assertThatCode(() -> notificationService.notifyOrderPlaced(order()))
                    .doesNotThrowAnyException();
        } finally {
            release.countDown();
        }
    }
}
