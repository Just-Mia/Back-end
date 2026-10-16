package com.example.printershop.domain.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record OrderResponse(Integer id, String customerUsername, OffsetDateTime createdAt,
                            List<OrderItemResponse> items) {
}
