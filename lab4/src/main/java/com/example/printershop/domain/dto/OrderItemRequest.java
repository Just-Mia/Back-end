package com.example.printershop.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderItemRequest(@NotNull Integer productId, @Min(1) int quantity) {
}
