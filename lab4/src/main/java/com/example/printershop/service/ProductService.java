package com.example.printershop.service;

import com.example.printershop.domain.dto.ProductRequest;
import com.example.printershop.domain.dto.ProductResponse;
import com.example.printershop.domain.mapper.ProductMapper;
import com.example.printershop.domain.model.Product;
import com.example.printershop.exception.ProductNotFoundException;
import com.example.printershop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Transactional(readOnly = true)
    public List<ProductResponse> listAll() {
        return productRepository.findAll().stream().map(productMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Integer id) {
        return productMapper.toResponse(findOrThrow(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product entity = productMapper.toEntity(request);
        return productMapper.toResponse(productRepository.save(entity));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ProductResponse update(Integer id, ProductRequest request) {
        Product entity = findOrThrow(id);
        productMapper.updateEntityFromRequest(request, entity);
        return productMapper.toResponse(productRepository.save(entity));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void delete(Integer id) {
        if (!productRepository.existsById(id)) throw new ProductNotFoundException(id);
        productRepository.deleteById(id);
    }

    private Product findOrThrow(Integer id) {
        return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }
}
