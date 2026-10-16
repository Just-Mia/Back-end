package com.example.printershop.integration;

import com.example.printershop.domain.dto.OrderItemResponse;
import com.example.printershop.domain.dto.OrderResponse;
import com.example.printershop.domain.dto.ShippingQuote;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class SimulatedShippingApi implements ShippingApi {

    private final long delayMs;

    public SimulatedShippingApi(@Value("${shop.simulated.shipping-delay-ms:1000}") long delayMs) {
        this.delayMs = delayMs;
    }

    @Override
    public ShippingQuote quote(OrderResponse order) {
        SimulatedDelay.sleep(delayMs);
        int totalQuantity = order.items().stream().mapToInt(OrderItemResponse::quantity).sum();
        return new ShippingQuote("Nova Poshta", 2, BigDecimal.valueOf(50L + 10L * totalQuantity));
    }
}
