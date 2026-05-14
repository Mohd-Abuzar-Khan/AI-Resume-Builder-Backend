package com.resumade.export.service;

import com.resumade.export.dto.ExportJobMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExportJobConsumerTest {

    @Mock
    private ExportService exportService;

    @InjectMocks
    private ExportJobConsumer exportJobConsumer;

    @Test
    void consumeMessage_shouldCallProcessExport() {
        UUID jobId = UUID.randomUUID();
        ExportJobMessage message = new ExportJobMessage();
        message.setJobId(jobId);

        exportJobConsumer.consumeMessage(message);

        verify(exportService).processExport(jobId);
    }
}
