package com.example.printershop.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("ProductNotFoundException -> 404")
    void productNotFound_returns404() {
        ResponseEntity<ApplicationErrorResponse> response = handler.handle(new ProductNotFoundException(10));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).isEqualTo("Product not found: 10");
    }

    @Test
    @DisplayName("OrderNotFoundException -> 404")
    void orderNotFound_returns404() {
        ResponseEntity<ApplicationErrorResponse> response = handler.handle(new OrderNotFoundException(20));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).isEqualTo("Order not found: 20");
    }

    @Test
    @DisplayName("AccessDeniedException -> 403")
    void accessDenied_returns403() {
        ResponseEntity<ApplicationErrorResponse> response = handler.handle(new AccessDeniedException("x"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getMessage()).isEqualTo("Access denied");
    }

    @Test
    @DisplayName("TooManyRequestsException -> 429")
    void tooManyRequests_returns429() {
        ResponseEntity<ApplicationErrorResponse> response = handler.handle(new TooManyRequestsException("slow down"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody().getMessage()).isEqualTo("slow down");
    }
}
