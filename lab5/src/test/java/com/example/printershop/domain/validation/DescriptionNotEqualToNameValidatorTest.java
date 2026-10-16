package com.example.printershop.domain.validation;

import com.example.printershop.domain.dto.ProductRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DescriptionNotEqualToNameValidatorTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    @DisplayName("AC1: валідатор підключений лише через @Constraint(validatedBy) і викликається Validator-ом")
    void validatorIsFoundViaConstraintAnnotation() {
        Constraint constraint = DescriptionNotEqualToName.class.getAnnotation(Constraint.class);
        assertThat(constraint.validatedBy()).containsExactly(DescriptionNotEqualToNameValidator.class);


        assertThat(validator.validate(new ProductRequest("Mouse", "mouse"))).hasSize(1);
    }

    @Test
    @DisplayName("AC2: опис відрізняється від назви - порушень немає")
    void differentDescription_noViolation() {
        assertThat(validator.validate(new ProductRequest("Mouse", "Wireless optical mouse"))).isEmpty();
    }

    @Test
    @DisplayName("AC2: опис не заданий - порушень немає")
    void missingDescription_noViolation() {
        assertThat(validator.validate(new ProductRequest("Mouse", null))).isEmpty();
    }

    @Test
    @DisplayName("AC3: опис збігається з назвою - порушення з чітким повідомленням")
    void descriptionEqualToName_violationWithClearMessage() {
        Set<ConstraintViolation<ProductRequest>> violations =
                validator.validate(new ProductRequest("Mouse", " MOUSE "));

        assertThat(violations).hasSize(1);
        ConstraintViolation<ProductRequest> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("Description must differ from the name, but both are 'Mouse'");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("description");
    }
}
