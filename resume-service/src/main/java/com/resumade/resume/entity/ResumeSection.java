package com.resumade.resume.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "resume_sections")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
// Individual resume section (experience, skills, etc.) — content stored as JSON string
public class ResumeSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer sectionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    @JsonIgnore
    private Resume resume;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SectionType sectionType;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content; // Raw JSON string — schema varies by sectionType (no server-side validation)

    @Column(nullable = false)
    private Integer displayOrder;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isVisible = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean aiGenerated = false; // Tracks whether this section was created by the AI service

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public ResumeSection(Resume resume, SectionType sectionType, String title, String content, Integer displayOrder) {
        this.resume = resume;
        this.sectionType = sectionType;
        this.title = title;
        this.content = content;
        this.displayOrder = displayOrder;
        this.isVisible = true;
        this.aiGenerated = false;
    }

    public enum SectionType {
        PERSONAL_INFO, SUMMARY, EXPERIENCE, EDUCATION, SKILLS, PROJECTS, CERTIFICATIONS, CUSTOM, ACHIEVEMENTS
    }
}
