package com.example.printershop.domain.dto;

import java.math.BigDecimal;

public record ShippingQuote(String carrier, int estimatedDays, BigDecimal price) {
}
