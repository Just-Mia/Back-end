package com.example.printershop.service;

import com.example.printershop.domain.dto.ProductRequest;
import com.example.printershop.domain.dto.ProductResponse;
import com.example.printershop.domain.mapper.ProductMapper;
import com.example.printershop.domain.model.Product;
import com.example.printershop.exception.ProductNotFoundException;
import com.example.printershop.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    @Test
    void listAll_returnsMappedProducts() {
        Product product = new Product(1, "Mouse", "Wireless");
        ProductResponse response = new ProductResponse(1, "Mouse", "Wireless");
        when(productRepository.findAll()).thenReturn(List.of(product));
        when(productMapper.toResponse(product)).thenReturn(response);

        assertThat(productService.listAll()).containsExactly(response);
    }

    @Test
    void getById_missing_throwsNotFound() {
        when(productRepository.findById(404)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(404)).isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void create_savesMappedEntity() {
        ProductRequest request = new ProductRequest("Mouse", "Wireless");
        Product entity = new Product(null, "Mouse", "Wireless");
        Product saved = new Product(1, "Mouse", "Wireless");
        ProductResponse response = new ProductResponse(1, "Mouse", "Wireless");
        when(productMapper.toEntity(request)).thenReturn(entity);
        when(productRepository.save(entity)).thenReturn(saved);
        when(productMapper.toResponse(saved)).thenReturn(response);

        assertThat(productService.create(request)).isEqualTo(response);
    }

    @Test
    void update_existing_appliesRequestToEntity() {
        ProductRequest request = new ProductRequest("New", "Desc");
        Product entity = new Product(1, "Old", "Old");
        when(productRepository.findById(1)).thenReturn(Optional.of(entity));
        when(productRepository.save(entity)).thenReturn(entity);
        when(productMapper.toResponse(entity)).thenReturn(new ProductResponse(1, "New", "Desc"));

        ProductResponse result = productService.update(1, request);

        verify(productMapper).updateEntityFromRequest(request, entity);
        assertThat(result.name()).isEqualTo("New");
    }

    @Test
    void delete_missing_throwsAndDoesNotDelete() {
        when(productRepository.existsById(404)).thenReturn(false);

        assertThatThrownBy(() -> productService.delete(404)).isInstanceOf(ProductNotFoundException.class);
        verify(productRepository, never()).deleteById(any());
    }

    @Test
    void delete_existing_deletes() {
        when(productRepository.existsById(1)).thenReturn(true);

        productService.delete(1);

        verify(productRepository).deleteById(1);
    }
}
