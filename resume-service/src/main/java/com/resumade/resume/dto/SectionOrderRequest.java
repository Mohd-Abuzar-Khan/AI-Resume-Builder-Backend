package com.resumade.resume.dto;

import jakarta.validation.constraints.NotNull;

public class SectionOrderRequest {

    @NotNull(message = "Section ID is required")
    private Integer sectionId;

    @NotNull(message = "Order is required")
    private Integer order;

    public Integer getSectionId() { return sectionId; }
    public void setSectionId(Integer sectionId) { this.sectionId = sectionId; }

    public Integer getOrder() { return order; }
    public void setOrder(Integer order) { this.order = order; }
}
