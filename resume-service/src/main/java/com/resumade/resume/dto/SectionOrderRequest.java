package com.resumade.resume.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionOrderRequest {

    @NotNull(message = "Section ID is required")
    private Integer sectionId;

    @NotNull(message = "Order is required")
    private Integer order;
}
