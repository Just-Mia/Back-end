package com.example.printershop.domain.mapper;

import com.example.printershop.domain.dto.OrderResponse;
import com.example.printershop.domain.model.Order;
import com.example.printershop.domain.model.OrderItem;
import com.example.printershop.domain.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OrderMapperImplTest {

    @Test
    void toResponse_mapsOrderAndNestedItems() {
        OrderMapperImpl mapper = new OrderMapperImpl();
        ReflectionTestUtils.setField(mapper, "orderItemMapper", new OrderItemMapperImpl());

        OrderItem item = new OrderItem();
        item.setProduct(new Product(7, "Mouse", "Wireless"));
        item.setQuantity(2);
        Order order = new Order();
        order.setId(42);
        order.setCustomerUsername("customer");
        order.setCreatedAt(OffsetDateTime.parse("2026-01-01T10:00:00Z"));
        order.addItem(item);

        OrderResponse response = mapper.toResponse(order);

        assertThat(response.id()).isEqualTo(42);
        assertThat(response.customerUsername()).isEqualTo("customer");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).productId()).isEqualTo(7);
        assertThat(response.items().get(0).productName()).isEqualTo("Mouse");
        assertThat(response.items().get(0).quantity()).isEqualTo(2);
    }

    @Test
    void toResponse_nullOrder_returnsNull() {
        OrderMapperImpl mapper = new OrderMapperImpl();

        assertThat(mapper.toResponse(null)).isNull();
    }
}
