package com.example.printershop.exception;


public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(Integer id) {
        super("Product not found: " + id);
    }
}