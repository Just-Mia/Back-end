package com.example.printershop.domain.mapper;

import com.example.printershop.domain.dto.ProductRequest;
import com.example.printershop.domain.dto.ProductResponse;
import com.example.printershop.domain.model.Product;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductMapperImplTest {

    private final ProductMapper mapper = new ProductMapperImpl();

    @Test
    void toResponse_mapsAllFields() {
        ProductResponse response = mapper.toResponse(new Product(1, "Mouse", "Wireless"));

        assertThat(response).isEqualTo(new ProductResponse(1, "Mouse", "Wireless"));
    }

    @Test
    void toEntity_mapsFieldsAndLeavesIdNull() {
        Product product = mapper.toEntity(new ProductRequest("Mouse", "Wireless"));

        assertThat(product.getId()).isNull();
        assertThat(product.getName()).isEqualTo("Mouse");
        assertThat(product.getDescription()).isEqualTo("Wireless");
    }

    @Test
    void updateEntityFromRequest_overwritesFieldsAndKeepsId() {
        Product product = new Product(1, "Old", "Old description");

        mapper.updateEntityFromRequest(new ProductRequest("New", null), product);

        assertThat(product.getId()).isEqualTo(1);
        assertThat(product.getName()).isEqualTo("New");
        assertThat(product.getDescription()).isEqualTo("Old description"); // null ігнорується
    }

    @Test
    void nullInput_returnsNull() {
        assertThat(mapper.toResponse(null)).isNull();
        assertThat(mapper.toEntity(null)).isNull();
    }
}
