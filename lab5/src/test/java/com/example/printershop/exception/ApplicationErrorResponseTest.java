package com.example.printershop.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;


class ApplicationErrorResponseTest {
    @Test
    @DisplayName("Конструктор зі String коректно встановлює повідомлення")
    void constructor_withStringMessage_setsMessage() {
        ApplicationErrorResponse response = new ApplicationErrorResponse("Custom error message");

        assertEquals("Custom error message", response.getMessage());
    }

    @Test
    @DisplayName("Конструктор з Exception коректно витягує повідомлення з винятку")
    void constructor_withException_setsMessageFromException() {
        RuntimeException ex = new RuntimeException("Exception occurred");

        ApplicationErrorResponse response = new ApplicationErrorResponse(ex);

        assertEquals("Exception occurred", response.getMessage());
    }
}
