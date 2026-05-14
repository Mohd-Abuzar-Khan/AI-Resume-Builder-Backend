package com.resumade.export.dto;

import com.resumade.export.entity.ExportJob;

import java.io.Serializable;
import java.util.UUID;

public class ExportJobMessage implements Serializable {
    private UUID jobId;
    private Integer userId;
    private Integer resumeId;
    private ExportJob.ExportFormat format;

    public ExportJobMessage() {}

    public ExportJobMessage(UUID jobId, Integer userId, Integer resumeId, ExportJob.ExportFormat format) {
        this.jobId = jobId;
        this.userId = userId;
        this.resumeId = resumeId;
        this.format = format;
    }

    public UUID getJobId() { return jobId; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getResumeId() { return resumeId; }
    public void setResumeId(Integer resumeId) { this.resumeId = resumeId; }

    public ExportJob.ExportFormat getFormat() { return format; }
    public void setFormat(ExportJob.ExportFormat format) { this.format = format; }
}
