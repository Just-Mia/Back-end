package com.example.printershop.controller;

import com.example.printershop.domain.annotation.CurrentUser;
import com.example.printershop.domain.dto.FulfillmentSummary;
import com.example.printershop.domain.dto.OrderResponse;
import com.example.printershop.service.OrderFulfillmentService;
import com.example.printershop.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderFulfillmentController {

    private final OrderService orderService;
    private final OrderFulfillmentService fulfillmentService;

    @GetMapping("/{id}/fulfillment")
    public FulfillmentSummary fulfillment(@PathVariable Integer id, @CurrentUser String username,
                                          Authentication auth) {
        boolean isAdmin = auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        OrderResponse order = orderService.getOrder(id, username, isAdmin);
        return fulfillmentService.prepare(order);
    }
}
