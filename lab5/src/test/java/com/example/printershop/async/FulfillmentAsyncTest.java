package com.example.printershop.async;

import com.example.printershop.async.AsyncTestSupport.StubPdfRenderer;
import com.example.printershop.async.AsyncTestSupport.StubShippingApi;
import com.example.printershop.configuration.AsyncConfig;
import com.example.printershop.domain.dto.FulfillmentSummary;
import com.example.printershop.service.InvoicePdfService;
import com.example.printershop.service.OrderFulfillmentService;
import com.example.printershop.service.ShippingQuoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.concurrent.CountDownLatch;

import static com.example.printershop.async.AsyncTestSupport.order;
import static org.assertj.core.api.Assertions.assertThat;


@SpringJUnitConfig(FulfillmentAsyncTest.Config.class)
@TestPropertySource(properties = "shop.async.wait-timeout-ms=700")
class FulfillmentAsyncTest {

    @Configuration
    @Import({AsyncConfig.class, InvoicePdfService.class, ShippingQuoteService.class, OrderFulfillmentService.class})
    static class Config {
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
    private OrderFulfillmentService fulfillmentService;
    @Autowired
    private StubPdfRenderer pdf;
    @Autowired
    private StubShippingApi shipping;

    @BeforeEach
    void setUp() {
        pdf.reset();
        shipping.reset();
    }

    @Test
    @DisplayName("обидва виклики виконуються одночасно, результати об'єднані thenCombine")
    void independentCallsRunConcurrently() {

        CountDownLatch bothRunning = new CountDownLatch(2);
        pdf.rendezvous = bothRunning;
        shipping.rendezvous = bothRunning;

        FulfillmentSummary summary = fulfillmentService.prepare(order());

        assertThat(summary.degraded()).isFalse();
        assertThat(summary.warnings()).isEmpty();
        assertThat(summary.invoice().fileName()).isEqualTo("invoice-1.pdf");
        assertThat(summary.invoice().sizeBytes()).isEqualTo(3);
        assertThat(summary.shipping().carrier()).isEqualTo("TestCarrier");
        assertThat(bothRunning.getCount()).isZero();
        assertThat(pdf.threadName).startsWith("shop-async-");
        assertThat(shipping.threadName).startsWith("shop-async-");
        assertThat(pdf.threadName).isNotEqualTo(shipping.threadName);
    }

    @Test
    @DisplayName("збій одного виклику: запит не падає, повертається часткова відповідь (exceptionally)")
    void oneFailureDoesNotFailTheCaller() {
        shipping.fail = true;

        FulfillmentSummary summary = fulfillmentService.prepare(order());

        assertThat(summary.degraded()).isTrue();
        assertThat(summary.invoice()).isNotNull();
        assertThat(summary.shipping()).isNull();
        assertThat(summary.warnings()).hasSize(1);
    }

    @Test
    @DisplayName("збій обох викликів: усе одно нормальна (деградована) відповідь, без винятку")
    void bothFailuresDoNotFailTheCaller() {
        pdf.fail = true;
        shipping.fail = true;

        FulfillmentSummary summary = fulfillmentService.prepare(order());

        assertThat(summary.degraded()).isTrue();
        assertThat(summary.invoice()).isNull();
        assertThat(summary.shipping()).isNull();
        assertThat(summary.warnings()).hasSize(2);
    }

    @Test
    @DisplayName("очікування обмежене: надто повільний виклик -> тайм-аут, часткова відповідь")
    void waitingIsBoundedByTimeout() {
        shipping.hold = new CountDownLatch(1);
        long start = System.nanoTime();
        try {
            FulfillmentSummary summary = fulfillmentService.prepare(order());

            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            assertThat(elapsedMs).isLessThan(5_000);
            assertThat(summary.degraded()).isTrue();
            assertThat(summary.invoice()).isNotNull();
            assertThat(summary.shipping()).isNull();
            assertThat(summary.warnings()).anyMatch(w -> w.contains("Timed out"));
        } finally {
            shipping.hold.countDown();
        }
    }
}
