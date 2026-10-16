package com.example.printershop.service;

import com.example.printershop.domain.dto.FulfillmentSummary;
import com.example.printershop.domain.dto.InvoiceInfo;
import com.example.printershop.domain.dto.OrderResponse;
import com.example.printershop.domain.dto.ShippingQuote;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;




@Slf4j
@Service
public class OrderFulfillmentService {

    private final InvoicePdfService invoicePdfService;
    private final ShippingQuoteService shippingQuoteService;
    private final long waitTimeoutMs;

    public OrderFulfillmentService(InvoicePdfService invoicePdfService,
                                   ShippingQuoteService shippingQuoteService,
                                   @Value("${shop.async.wait-timeout-ms:3000}") long waitTimeoutMs) {
        this.invoicePdfService = invoicePdfService;
        this.shippingQuoteService = shippingQuoteService;
        this.waitTimeoutMs = waitTimeoutMs;
    }



    public FulfillmentSummary prepare(OrderResponse order) {

        CompletableFuture<InvoiceInfo> invoiceCall = invoicePdfService.generateInvoice(order);
        CompletableFuture<ShippingQuote> shippingCall;
        try {
            shippingCall = shippingQuoteService.fetchQuote(order);
        } catch (TaskRejectedException e) {
            invoiceCall.cancel(true);
            throw e;
        }

        List<String> warnings = new CopyOnWriteArrayList<>();
        CompletableFuture<InvoiceInfo> invoice = invoiceCall.exceptionally(ex -> {
            warnings.add("Invoice PDF is temporarily unavailable");
            log.warn("[Fulfillment] invoice for order {} failed: {}", order.id(), rootMessage(ex));
            return null;
        });
        CompletableFuture<ShippingQuote> shipping = shippingCall.exceptionally(ex -> {
            warnings.add("Shipping quote is temporarily unavailable");
            log.warn("[Fulfillment] shipping quote for order {} failed: {}", order.id(), rootMessage(ex));
            return null;
        });

        CompletableFuture<FulfillmentSummary> combined = invoice.thenCombine(shipping,
                (inv, ship) -> FulfillmentSummary.of(order.id(), inv, ship, warnings));

        try {
            return combined.get(waitTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.warn("[Fulfillment] order {}: gave up waiting after {} ms", order.id(), waitTimeoutMs);
            invoiceCall.cancel(true);
            shippingCall.cancel(true);
            List<String> reasons = new ArrayList<>(warnings);
            reasons.add("Timed out after " + waitTimeoutMs + " ms");
            return FulfillmentSummary.of(order.id(), invoice.getNow(null), shipping.getNow(null), reasons);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return FulfillmentSummary.of(order.id(), null, null, List.of("Interrupted while waiting"));
        } catch (ExecutionException e) {
            log.warn("[Fulfillment] order {}: combining failed: {}", order.id(), rootMessage(e));
            return FulfillmentSummary.of(order.id(), null, null, List.of("Fulfillment preparation failed"));
        }
    }

    private static String rootMessage(Throwable ex) {
        Throwable t = ex;
        while ((t instanceof CompletionException || t instanceof ExecutionException) && t.getCause() != null) {
            t = t.getCause();
        }
        return t.getClass().getSimpleName() + ": " + t.getMessage();
    }
}
