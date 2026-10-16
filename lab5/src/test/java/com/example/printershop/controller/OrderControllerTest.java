package com.example.printershop.controller;

import com.example.printershop.configuration.CurrentUserArgumentResolver;
import com.example.printershop.domain.dto.OrderResponse;
import com.example.printershop.domain.dto.PlaceOrderRequest;
import com.example.printershop.exception.GlobalExceptionHandler;
import com.example.printershop.service.OrderNotificationService;
import com.example.printershop.service.OrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private OrderNotificationService orderNotificationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new OrderController(orderService, orderNotificationService))
                .setCustomArgumentResolvers(new CurrentUserArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        TestingAuthenticationToken auth = new TestingAuthenticationToken("customer", "n/a", "ROLE_USER");
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("AC8: ім'я з @CurrentUser реально потрапляє в сервіс і у відповідь")
    void placeOrder_usesResolvedUsername() throws Exception {
        when(orderService.placeOrder(eq("customer"), any(PlaceOrderRequest.class)))
                .thenReturn(new OrderResponse(1, "customer", OffsetDateTime.now(), List.of()));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":1,\"quantity\":2}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerUsername").value("customer"));

        verify(orderService).placeOrder(eq("customer"), any(PlaceOrderRequest.class));
    }

    @Test
    @DisplayName("Lab 5: після створення замовлення запускається фонове сповіщення (fire-and-forget)")
    void placeOrder_triggersNotification() throws Exception {
        OrderResponse created = new OrderResponse(1, "customer", OffsetDateTime.now(), List.of());
        when(orderService.placeOrder(eq("customer"), any(PlaceOrderRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":1,\"quantity\":2}]}"))
                .andExpect(status().isOk());

        verify(orderNotificationService).notifyOrderPlaced(created);
    }

    @Test
    @DisplayName("AC8: GET /mine використовує резолвлене ім'я")
    void myOrders_usesResolvedUsername() throws Exception {
        when(orderService.listMyOrders("customer"))
                .thenReturn(List.of(new OrderResponse(5, "customer", OffsetDateTime.now(), List.of())));

        mockMvc.perform(get("/api/orders/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].customerUsername").value("customer"));
    }

    @Test
    @DisplayName("Порожній список позицій -> 400 (працює @Valid)")
    void placeOrder_emptyItems_returns400() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[]}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService, orderNotificationService);
    }
}
