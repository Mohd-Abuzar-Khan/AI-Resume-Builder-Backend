package com.resumade.export.service;

import com.resumade.export.dto.ExportJobMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class ExportJobConsumer {

    private static final Logger log = LoggerFactory.getLogger(ExportJobConsumer.class);
    private final ExportService exportService;

    public ExportJobConsumer(ExportService exportService) {
        this.exportService = exportService;
    }

    @RabbitListener(queues = "${export.queue.name}")
    public void consumeMessage(ExportJobMessage message) {
        log.info("Received export job message: {}", message.getJobId());
        exportService.processExport(message.getJobId());
    }
}
