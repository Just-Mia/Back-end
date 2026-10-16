package com.example.printershop.async;

import com.example.printershop.domain.dto.OrderItemResponse;
import com.example.printershop.domain.dto.OrderResponse;
import com.example.printershop.domain.dto.ShippingQuote;
import com.example.printershop.integration.MailGateway;
import com.example.printershop.integration.PdfRenderer;
import com.example.printershop.integration.ShippingApi;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;


final class AsyncTestSupport {

    private AsyncTestSupport() {
    }

    static OrderResponse order() {
        return new OrderResponse(1, "customer", OffsetDateTime.now(),
                List.of(new OrderItemResponse(1, "Printer", 2)));
    }

    static boolean await(CountDownLatch latch) throws InterruptedException {
        return latch.await(5, TimeUnit.SECONDS);
    }

    static void waitUntil(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
    }


    static class ControllableMailGateway implements MailGateway {
        volatile CountDownLatch started = new CountDownLatch(1);
        volatile CountDownLatch release = new CountDownLatch(0);
        volatile CountDownLatch finished = new CountDownLatch(1);
        volatile boolean fail;
        volatile String threadName;

        void reset() {
            started = new CountDownLatch(1);
            release = new CountDownLatch(0);
            finished = new CountDownLatch(1);
            fail = false;
            threadName = null;
        }

        @Override
        public void send(String to, String subject, String body) {
            threadName = Thread.currentThread().getName();
            started.countDown();
            try {
                if (!release.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("mail was never released");
                }
                if (fail) {
                    throw new IllegalStateException("smtp is down");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            } finally {
                finished.countDown();
            }
        }
    }


    abstract static class ControllableCall {

        volatile CountDownLatch rendezvous = new CountDownLatch(0);
        volatile CountDownLatch hold = new CountDownLatch(0);
        volatile boolean fail;
        volatile String threadName;

        void reset() {
            rendezvous = new CountDownLatch(0);
            hold = new CountDownLatch(0);
            fail = false;
            threadName = null;
        }

        void run() {
            threadName = Thread.currentThread().getName();
            try {
                rendezvous.countDown();
                if (!rendezvous.await(2, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("the two calls did not run concurrently");
                }
                if (!hold.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("call was never released");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
            if (fail) {
                throw new IllegalStateException("downstream is down");
            }
        }
    }

    static class StubPdfRenderer extends ControllableCall implements PdfRenderer {
        @Override
        public byte[] render(OrderResponse order) {
            run();
            return new byte[]{1, 2, 3};
        }
    }

    static class StubShippingApi extends ControllableCall implements ShippingApi {
        @Override
        public ShippingQuote quote(OrderResponse order) {
            run();
            return new ShippingQuote("TestCarrier", 1, BigDecimal.TEN);
        }
    }
}
