package com.example.printershop.service;

import com.example.printershop.domain.annotation.CountFailures;
import com.example.printershop.domain.annotation.RateLimited;
import com.example.printershop.domain.annotation.RetryOnConflict;
import com.example.printershop.domain.dto.OrderItemRequest;
import com.example.printershop.domain.dto.OrderResponse;
import com.example.printershop.domain.dto.PlaceOrderRequest;
import com.example.printershop.domain.mapper.OrderMapper;
import com.example.printershop.domain.model.Order;
import com.example.printershop.domain.model.OrderItem;
import com.example.printershop.domain.model.Product;
import com.example.printershop.exception.OrderNotFoundException;
import com.example.printershop.exception.ProductNotFoundException;
import com.example.printershop.repository.OrderRepository;
import com.example.printershop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    @CountFailures
    @RateLimited(maxCalls = 5, perSeconds = 60)
    @RetryOnConflict(maxAttempts = 3, backoffMs = 100)
    @Transactional
    public OrderResponse placeOrder(String customerUsername, PlaceOrderRequest request) {
        Order order = new Order();
        order.setCustomerUsername(customerUsername);
        order.setCreatedAt(OffsetDateTime.now());

        for (OrderItemRequest itemRequest : request.items()) {
            Product product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> new ProductNotFoundException(itemRequest.productId()));

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(itemRequest.quantity());
            order.addItem(orderItem);
        }

        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listMyOrders(String customerUsername) {
        return orderRepository.findByCustomerUsername(customerUsername).stream()
                .map(orderMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Integer orderId, String requestingUsername, boolean isAdmin) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        if (!isAdmin && !order.getCustomerUsername().equals(requestingUsername)) {
            throw new AccessDeniedException("You do not have permission to view this order");
        }
        return orderMapper.toResponse(order);
    }
}
