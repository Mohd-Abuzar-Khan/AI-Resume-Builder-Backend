package com.resumade.export.service;

import com.resumade.export.entity.ExportJob;
import java.util.List;
import java.util.UUID;

public interface ExportService {
    ExportJob createExportJob(Integer userId, Integer resumeId, ExportJob.ExportFormat format);
    ExportJob getJobStatus(UUID jobId);
    List<ExportJob> getUserHistory(Integer userId);
    void processExport(UUID jobId);
}
