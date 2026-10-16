package com.example.printershop.service;

import com.example.printershop.domain.dto.OrderItemRequest;
import com.example.printershop.domain.dto.OrderResponse;
import com.example.printershop.domain.dto.PlaceOrderRequest;
import com.example.printershop.domain.mapper.OrderMapper;
import com.example.printershop.domain.model.Order;
import com.example.printershop.domain.model.Product;
import com.example.printershop.exception.OrderNotFoundException;
import com.example.printershop.exception.ProductNotFoundException;
import com.example.printershop.repository.OrderRepository;
import com.example.printershop.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderService orderService;

    @Test
    void placeOrder_savesOrderForUsernameWithItems() {
        Product product = new Product(1, "Mouse", "Wireless");
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        OrderResponse expected = new OrderResponse(1, "customer", null, List.of());
        when(orderMapper.toResponse(any(Order.class))).thenReturn(expected);

        OrderResponse result = orderService.placeOrder("customer",
                new PlaceOrderRequest(List.of(new OrderItemRequest(1, 3))));

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        Order saved = captor.getValue();
        assertThat(saved.getCustomerUsername()).isEqualTo("customer");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getItems()).hasSize(1);
        assertThat(saved.getItems().get(0).getProduct()).isSameAs(product);
        assertThat(saved.getItems().get(0).getQuantity()).isEqualTo(3);
        assertThat(saved.getItems().get(0).getOrder()).isSameAs(saved);
        assertThat(result).isSameAs(expected);
    }

    @Test
    void placeOrder_unknownProduct_throwsAndSavesNothing() {
        when(productRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.placeOrder("customer",
                new PlaceOrderRequest(List.of(new OrderItemRequest(99, 1)))))
                .isInstanceOf(ProductNotFoundException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void getOrder_missing_throwsNotFound() {
        when(orderRepository.findById(5)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(5, "customer", false))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void getOrder_otherUsersOrder_forbiddenForNonAdmin() {
        Order order = new Order();
        order.setCustomerUsername("someone-else");
        when(orderRepository.findById(5)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getOrder(5, "customer", false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getOrder_otherUsersOrder_allowedForAdmin() {
        Order order = new Order();
        order.setCustomerUsername("someone-else");
        OrderResponse expected = new OrderResponse(5, "someone-else", null, List.of());
        when(orderRepository.findById(5)).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(expected);

        assertThat(orderService.getOrder(5, "admin", true)).isSameAs(expected);
    }

    @Test
    void listMyOrders_mapsRepositoryResult() {
        Order order = new Order();
        OrderResponse response = new OrderResponse(1, "customer", null, List.of());
        when(orderRepository.findByCustomerUsername("customer")).thenReturn(List.of(order));
        when(orderMapper.toResponse(order)).thenReturn(response);

        assertThat(orderService.listMyOrders("customer")).containsExactly(response);
    }
}
