package com.example.printershop.domain.dto;

import com.example.printershop.domain.validation.DescriptionNotEqualToName;
import jakarta.validation.constraints.NotBlank;

@DescriptionNotEqualToName
public record ProductRequest(@NotBlank String name, String description) {
}
