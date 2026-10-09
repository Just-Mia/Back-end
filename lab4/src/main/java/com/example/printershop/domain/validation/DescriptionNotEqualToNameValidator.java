package com.example.printershop.domain.validation;

import com.example.printershop.domain.dto.ProductRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DescriptionNotEqualToNameValidator
        implements ConstraintValidator<DescriptionNotEqualToName, ProductRequest> {

    @Override
    public boolean isValid(ProductRequest request, ConstraintValidatorContext context) {
        if (request == null || request.name() == null || request.description() == null
                || request.description().isBlank()) {
            return true;
        }

        if (!request.name().trim().equalsIgnoreCase(request.description().trim())) {
            return true;
        }

        context.disableDefaultConstraintViolation();

        String safeName = request.name().trim().replaceAll("[\\\\{}$]", "");
        context.buildConstraintViolationWithTemplate(
                        "Description must differ from the name, but both are '" + safeName + "'")
                .addPropertyNode("description")
                .addConstraintViolation();
        return false;
    }
}
