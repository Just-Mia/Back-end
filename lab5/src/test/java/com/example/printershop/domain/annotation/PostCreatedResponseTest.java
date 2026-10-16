package com.example.printershop.domain.annotation;

import com.example.printershop.controller.ProductController;
import com.example.printershop.domain.dto.ProductRequest;
import com.example.printershop.domain.dto.ProductResponse;
import com.example.printershop.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PostCreatedResponseTest {

    @Mock
    private ProductService productService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProductController(productService)).build();
    }

    @Test
    @DisplayName("AC4: композитна анотація містить і @PostMapping (POST), і @ResponseStatus(CREATED)")
    void composedAnnotation_carriesBothOriginalAnnotations() throws Exception {
        Method create = ProductController.class.getMethod("create", ProductRequest.class);

        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(create, RequestMapping.class);
        ResponseStatus responseStatus = AnnotatedElementUtils.findMergedAnnotation(create, ResponseStatus.class);

        assertThat(mapping).isNotNull();
        assertThat(mapping.method()).containsExactly(RequestMethod.POST);
        assertThat(responseStatus).isNotNull();
        assertThat(responseStatus.code()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    @DisplayName("AC5: POST /api/products - коректний routing і статус 201 Created")
    void post_isRoutedAndReturns201() throws Exception {
        when(productService.create(any())).thenReturn(new ProductResponse(1, "Mouse", "Wireless mouse"));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mouse\",\"description\":\"Wireless mouse\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Mouse"));
    }

    @Test
    @DisplayName("AC5: композитна анотація мапить лише POST (PUT на колекцію -> 405)")
    void otherHttpMethods_areNotMapped() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/products"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(get("/api/products")).andExpect(status().isOk());
    }
}
