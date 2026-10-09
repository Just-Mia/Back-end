package com.example.printershop.controller;

import com.example.printershop.domain.dto.ProductResponse;
import com.example.printershop.exception.GlobalExceptionHandler;
import com.example.printershop.exception.ProductNotFoundException;
import com.example.printershop.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProductController(productService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/products повертає список")
    void listAll_returnsProducts() throws Exception {
        when(productService.listAll()).thenReturn(List.of(new ProductResponse(1, "Mouse", "Wireless mouse")));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Mouse"));
    }

    @Test
    @DisplayName("GET /api/products/{id} - неіснуючий -> 404")
    void getById_missing_returns404() throws Exception {
        when(productService.getById(404)).thenThrow(new ProductNotFoundException(404));

        mockMvc.perform(get("/api/products/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found: 404"));
    }

    @Test
    @DisplayName("Тип 1 через реальний запит: опис == назва -> 400 з поясненням")
    void create_descriptionEqualsName_returns400WithExplanation() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mouse\",\"description\":\"mouse\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("description: Description must differ from the name, but both are 'Mouse'"));

        verifyNoInteractions(productService);
    }

    @Test
    @DisplayName("Порожня назва -> 400")
    void create_blankName_returns400() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"description\":\"x\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE -> 204 No Content")
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/products/1")).andExpect(status().isNoContent());

        verify(productService).delete(1);
    }
}
