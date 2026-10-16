package com.example.printershop.domain.mapper;


import com.example.printershop.domain.dto.OrderResponse;
import com.example.printershop.domain.model.Order;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = OrderItemMapper.class)
public interface OrderMapper {
    OrderResponse toResponse(Order order);
}