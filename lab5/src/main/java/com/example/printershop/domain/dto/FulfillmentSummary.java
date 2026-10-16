package com.example.printershop.domain.dto;

import java.util.List;


public record FulfillmentSummary(Integer orderId, InvoiceInfo invoice, ShippingQuote shipping,
                                 boolean degraded, List<String> warnings) {

    public static FulfillmentSummary of(Integer orderId, InvoiceInfo invoice, ShippingQuote shipping,
                                        List<String> warnings) {
        return new FulfillmentSummary(orderId, invoice, shipping,
                invoice == null || shipping == null, List.copyOf(warnings));
    }
}
