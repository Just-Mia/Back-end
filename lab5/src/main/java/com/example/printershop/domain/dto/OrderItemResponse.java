package com.example.printershop.domain.dto;

public record OrderItemResponse(Integer productId, String productName, int quantity) {
}
