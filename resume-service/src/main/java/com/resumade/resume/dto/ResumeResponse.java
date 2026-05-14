package com.resumade.resume.dto;

import com.resumade.resume.entity.Resume.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeResponse {
    private Integer resumeId;
    private Integer userId;
    private String title;
    private String targetJobTitle;
    private Integer templateId;
    private Integer atsScore;
    private Status status;
    private String language;
    private Boolean isPublic;
    private Integer viewCount;
    private String ownerName;
    private String ownerAvatar;
    private List<SectionResponse> sections;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
