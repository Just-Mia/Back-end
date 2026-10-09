package com.example.printershop.controller;

import com.example.printershop.domain.annotation.CurrentUser;
import com.example.printershop.domain.dto.OrderResponse;
import com.example.printershop.domain.dto.PlaceOrderRequest;
import com.example.printershop.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public OrderResponse placeOrder(@Valid @RequestBody PlaceOrderRequest request, @CurrentUser String username) {
        return orderService.placeOrder(username, request);
    }

    @GetMapping("/mine")
    public List<OrderResponse> myOrders(@CurrentUser String username) {
        return orderService.listMyOrders(username);
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable Integer id, @CurrentUser String username, Authentication auth) {
        boolean isAdmin = auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        return orderService.getOrder(id, username, isAdmin);
    }
}
