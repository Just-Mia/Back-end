package com.example.printershop.controller;

import com.example.printershop.configuration.CurrentUserArgumentResolver;
import com.example.printershop.domain.dto.FulfillmentSummary;
import com.example.printershop.domain.dto.InvoiceInfo;
import com.example.printershop.domain.dto.OrderResponse;
import com.example.printershop.domain.dto.ShippingQuote;
import com.example.printershop.exception.GlobalExceptionHandler;
import com.example.printershop.service.OrderFulfillmentService;
import com.example.printershop.service.OrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrderFulfillmentControllerTest {

    @Mock
    private OrderService orderService;
    @Mock
    private OrderFulfillmentService fulfillmentService;

    private MockMvc mockMvc;
    private TestingAuthenticationToken auth;
    private final OrderResponse order = new OrderResponse(3, "customer", OffsetDateTime.now(), List.of());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new OrderFulfillmentController(orderService, fulfillmentService))
                .setCustomArgumentResolvers(new CurrentUserArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        auth = new TestingAuthenticationToken("customer", "n/a", "ROLE_USER");
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("200: повертає об'єднаний результат двох асинхронних викликів")
    void fulfillment_returnsCombinedSummary() throws Exception {
        when(orderService.getOrder(3, "customer", false)).thenReturn(order);
        when(fulfillmentService.prepare(order)).thenReturn(FulfillmentSummary.of(3,
                new InvoiceInfo("invoice-3.pdf", 100),
                new ShippingQuote("Nova Poshta", 2, BigDecimal.valueOf(70)), List.of()));

        mockMvc.perform(get("/api/orders/3/fulfillment").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(3))
                .andExpect(jsonPath("$.invoice.fileName").value("invoice-3.pdf"))
                .andExpect(jsonPath("$.shipping.carrier").value("Nova Poshta"))
                .andExpect(jsonPath("$.degraded").value(false));
    }

    @Test
    @DisplayName("переповнений пул (TaskRejectedException) -> 503 + Retry-After, а НЕ 500")
    void fulfillment_rejectedTask_returns503() throws Exception {
        when(orderService.getOrder(3, "customer", false)).thenReturn(order);
        when(fulfillmentService.prepare(any())).thenThrow(new TaskRejectedException("pool is full"));

        mockMvc.perform(get("/api/orders/3/fulfillment").principal(auth))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "5"))
                .andExpect(jsonPath("$.message").value("Сервер зараз перевантажений, спробуйте пізніше"));
    }
}
