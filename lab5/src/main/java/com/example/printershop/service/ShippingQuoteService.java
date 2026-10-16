package com.example.printershop.service;

import com.example.printershop.configuration.AsyncConfig;
import com.example.printershop.domain.dto.OrderResponse;
import com.example.printershop.domain.dto.ShippingQuote;
import com.example.printershop.integration.ShippingApi;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class ShippingQuoteService {

    private final ShippingApi shippingApi;

    @Async(AsyncConfig.EXECUTOR)
    public CompletableFuture<ShippingQuote> fetchQuote(OrderResponse order) {
        return CompletableFuture.completedFuture(shippingApi.quote(order));
    }
}
