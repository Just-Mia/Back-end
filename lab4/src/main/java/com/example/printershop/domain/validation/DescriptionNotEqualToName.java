package com.example.printershop.domain.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Documented
@Constraint(validatedBy = DescriptionNotEqualToNameValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DescriptionNotEqualToName {

    String message() default "Product description must not be identical to the product name";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
