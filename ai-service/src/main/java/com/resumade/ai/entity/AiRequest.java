package com.resumade.ai.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_requests")
public class AiRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID requestId;

    @Column(nullable = true)
    private Integer userId;

    private Integer resumeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestType requestType;

    @Column(columnDefinition = "TEXT")
    private String inputPrompt;

    @Column(columnDefinition = "TEXT")
    private String aiResponse;

    @Enumerated(EnumType.STRING)
    private ModelType model;



    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    private Long tokensUsed;

    public AiRequest() {}

    public AiRequest(Integer userId, Integer resumeId, RequestType requestType, String inputPrompt, RequestStatus status, ModelType model) {
        this.userId = userId;
        this.resumeId = resumeId;
        this.requestType = requestType;
        this.inputPrompt = inputPrompt;
        this.status = status;
        this.model = model;
    }

    // Getters and Setters
    public UUID getRequestId() { return requestId; }
    public void setRequestId(UUID requestId) { this.requestId = requestId; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getResumeId() { return resumeId; }
    public void setResumeId(Integer resumeId) { this.resumeId = resumeId; }

    public RequestType getRequestType() { return requestType; }
    public void setRequestType(RequestType requestType) { this.requestType = requestType; }

    public String getInputPrompt() { return inputPrompt; }
    public void setInputPrompt(String inputPrompt) { this.inputPrompt = inputPrompt; }

    public String getAiResponse() { return aiResponse; }
    public void setAiResponse(String aiResponse) { this.aiResponse = aiResponse; }

    public ModelType getModel() { return model; }
    public void setModel(ModelType model) { this.model = model; }



    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public Long getTokensUsed() { return tokensUsed; }
    public void setTokensUsed(Long tokensUsed) { this.tokensUsed = tokensUsed; }

    public enum RequestType {
        SUMMARY, BULLETS, COVER_LETTER, IMPROVE, ATS, SKILLS, TAILOR, TRANSLATE
    }

    public enum ModelType {
        GEMINI  // Only Gemini is supported
    }

    public enum RequestStatus {
        QUEUED, COMPLETED, FAILED
    }

    public static AiRequestBuilder builder() {
        return new AiRequestBuilder();
    }

    public static class AiRequestBuilder {
        private Integer userId;
        private Integer resumeId;
        private RequestType requestType;
        private String inputPrompt;
        private RequestStatus status;
        private ModelType model;
        private Long tokensUsed;

        public AiRequestBuilder userId(Integer userId) { this.userId = userId; return this; }
        public AiRequestBuilder resumeId(Integer resumeId) { this.resumeId = resumeId; return this; }
        public AiRequestBuilder requestType(RequestType requestType) { this.requestType = requestType; return this; }
        public AiRequestBuilder inputPrompt(String inputPrompt) { this.inputPrompt = inputPrompt; return this; }
        public AiRequestBuilder status(RequestStatus status) { this.status = status; return this; }
        public AiRequestBuilder model(ModelType model) { this.model = model; return this; }
        public AiRequestBuilder tokensUsed(Long tokensUsed) { this.tokensUsed = tokensUsed; return this; }
        public AiRequest build() {
            AiRequest request = new AiRequest(userId, resumeId, requestType, inputPrompt, status, model);
            request.setTokensUsed(tokensUsed);
            return request;
        }
    }
}
