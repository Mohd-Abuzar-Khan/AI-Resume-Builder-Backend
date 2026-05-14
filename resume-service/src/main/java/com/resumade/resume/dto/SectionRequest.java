package com.resumade.resume.dto;

import com.resumade.resume.entity.ResumeSection.SectionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionRequest {

    @NotNull(message = "Section Type is required")
    private SectionType sectionType;

    @NotBlank(message = "Title is required")
    private String title;

    private String content;

    private Integer displayOrder;
}
