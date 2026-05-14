package com.resumade.resume.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "cover_letters")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoverLetter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer userId;

    private Integer resumeId;

    @Column(nullable = false)
    private String title;

    private String jobTitle;

    private String company;

    @Column(columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public CoverLetter(Integer userId, Integer resumeId, String title, String jobTitle, String company, String content) {
        this.userId = userId;
        this.resumeId = resumeId;
        this.title = title;
        this.jobTitle = jobTitle;
        this.company = company;
        this.content = content;
    }
}
