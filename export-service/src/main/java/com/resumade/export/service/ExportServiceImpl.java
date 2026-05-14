package com.resumade.export.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;
import com.resumade.export.dto.ExportJobMessage;
import com.resumade.export.entity.ExportJob;
import com.resumade.export.repository.ExportRepository;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
// Async export pipeline: queues jobs via RabbitMQ, fetches resume data, generates PDF/DOCX/JSON files
public class ExportServiceImpl implements ExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportServiceImpl.class);

    private final ExportRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final TemplateEngine templateEngine;

    @Value("${export.queue.exchange}")
    private String exchange;

    @Value("${export.queue.routing-key}")
    private String routingKey;

    private static final String EXPORT_DIR = "exports/"; // WARNING: Relative path — depends on container working directory

    public ExportServiceImpl(ExportRepository repository, RabbitTemplate rabbitTemplate, RestTemplate restTemplate, ObjectMapper objectMapper, TemplateEngine templateEngine) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.templateEngine = templateEngine;
        
        // Ensure export directory exists
        File dir = new File(EXPORT_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    @Override
    public ExportJob createExportJob(Integer userId, Integer resumeId, ExportJob.ExportFormat format) {
        // Rate limit: 10 exports per user per calendar day (server timezone)
        long count = repository.countByUserIdToday(userId, LocalDateTime.now().withHour(0).withMinute(0));
        if (count >= 10) {
            throw new RuntimeException("Daily export limit reached (10/day).");
        }

        ExportJob job = new ExportJob(resumeId, userId, format, ExportJob.ExportStatus.QUEUED);
        job.setExpiresAt(LocalDateTime.now().plusDays(7));
        repository.save(job);

        ExportJobMessage message = new ExportJobMessage(job.getJobId(), userId, resumeId, format);
        rabbitTemplate.convertAndSend(exchange, routingKey, message);

        log.info("Export job created and queued: {}", job.getJobId());
        return job;
    }

    @Override
    public ExportJob getJobStatus(UUID jobId) {
        return repository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
    }

    @Override
    public List<ExportJob> getUserHistory(Integer userId) {
        return repository.findByUserId(userId);
    }

    @Override
    public void processExport(UUID jobId) {
        ExportJob job = repository.findById(jobId).orElse(null);
        if (job == null) return;

        job.setStatus(ExportJob.ExportStatus.PROCESSING);
        repository.save(job);

        try {
            log.info("Processing export job: {} format: {}", jobId, job.getFormat());
            
            // 1. Fetch Resume Data
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Id", String.valueOf(job.getUserId()));
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    "http://resume-service/api/v1/resumes/" + job.getResumeId(),
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            
            JsonNode resumeData = objectMapper.readTree(response.getBody());
            
            // 2. Generate File
            String extension = job.getFormat().name().toLowerCase();
            String filename = jobId.toString() + "." + extension;
            File outputFile = new File(EXPORT_DIR + filename);
            
            switch (job.getFormat()) {
                case PDF:
                    generatePdf(resumeData, outputFile);
                    break;
                case DOCX:
                    generateDocx(resumeData, outputFile);
                    break;
                case JSON:
                    generateJson(resumeData, outputFile);
                    break;
            }

            job.setStatus(ExportJob.ExportStatus.COMPLETED);
            // In a real app, this would be an S3 URL. For now, it's an API endpoint.
            job.setFileUrl("/api/v1/exports/download/" + filename); // Local path only — needs S3/CDN for production
            job.setFileSizeKb(outputFile.length() / 1024);
            job.setCompletedAt(LocalDateTime.now());
            
            log.info("Export completed for job: {}", jobId);
        } catch (Exception e) {
            log.error("Export processing failed for job {}: {}", jobId, e.getMessage(), e);
            job.setStatus(ExportJob.ExportStatus.FAILED);
        } finally {
            repository.save(job);
        }
    }
    
    private void generatePdf(JsonNode resumeData, File outputFile) throws Exception {
        Context context = new Context();
        context.setVariable("resume", resumeData);

        String htmlContent = templateEngine.process("resume", context);

        try (FileOutputStream os = new FileOutputStream(outputFile)) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            renderer.createPDF(os);
        }
    }
    
    private void generateDocx(JsonNode resumeData, File outputFile) throws Exception {
        try (XWPFDocument document = new XWPFDocument(); FileOutputStream out = new FileOutputStream(outputFile)) {
            
            XWPFParagraph titlePara = document.createParagraph();
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setBold(true);
            titleRun.setFontSize(20);
            String title = resumeData.has("title") ? resumeData.get("title").asText() : "Resume";
            titleRun.setText(title);
            
            if (resumeData.has("sections") && resumeData.get("sections").isArray()) {
                for (JsonNode section : resumeData.get("sections")) {
                    XWPFParagraph secTitlePara = document.createParagraph();
                    XWPFRun secTitleRun = secTitlePara.createRun();
                    secTitleRun.setBold(true);
                    secTitleRun.setFontSize(14);
                    if (section.has("title")) {
                        secTitleRun.setText(section.get("title").asText().toUpperCase());
                    }
                    
                    if (section.has("content")) {
                        XWPFParagraph contentPara = document.createParagraph();
                        XWPFRun contentRun = contentPara.createRun();
                        contentRun.setFontSize(12);
                        
                        String contentStr = section.get("content").asText();
                        try {
                            JsonNode contentJson = objectMapper.readTree(contentStr);
                            contentRun.setText(contentJson.toPrettyString().replaceAll("[{}\"]", ""));
                        } catch (Exception e) {
                            contentRun.setText(contentStr);
                        }
                    }
                }
            }
            
            document.write(out);
        }
    }
    
    private void generateJson(JsonNode resumeData, File outputFile) throws Exception {
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, resumeData);
    }
}
