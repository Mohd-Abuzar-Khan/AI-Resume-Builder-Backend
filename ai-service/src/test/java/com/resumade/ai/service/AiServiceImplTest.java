package com.resumade.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumade.ai.dto.AtsReport;
import com.resumade.ai.entity.AiRequest;
import com.resumade.ai.repository.AiRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiServiceImplTest {

    @Mock
    private WebClient.Builder webClientBuilder;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();
    @Mock
    private AiRequestRepository repository;

    @Mock
    private WebClient webClient;
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock
    private WebClient.RequestBodySpec requestBodySpec;
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;
    @Mock
    private jakarta.servlet.http.HttpServletRequest currentRequest;

    @InjectMocks
    private AiServiceImpl aiService;

    @BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(aiService, "geminiKey", "test-key");
        org.springframework.test.util.ReflectionTestUtils.setField(aiService, "geminiModel", "gemini-1.5-flash");
    }

    private void setupWebClientMocking() {
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void generateSummary_shouldReturnText() {
        setupWebClientMocking();

        Map<String, Object> geminiResponse = new HashMap<>();
        Map<String, Object> candidate = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", "{\"options\": [\"Generated summary\"]}");
        content.put("parts", Arrays.asList(part));
        candidate.put("content", content);
        geminiResponse.put("candidates", Arrays.asList(candidate));

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(geminiResponse));

        String result = aiService.generateSummary(1, 1, "Dev", 5);
        assertEquals("{\"options\": [\"Generated summary\"]}", result);
    }

    @Test
    void generateBulletPoints_shouldReturnText() {
        setupWebClientMocking();

        Map<String, Object> geminiResponse = new HashMap<>();
        Map<String, Object> candidate = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", "- Bullet 1\n- Bullet 2");
        content.put("parts", Arrays.asList(part));
        candidate.put("content", content);
        geminiResponse.put("candidates", Arrays.asList(candidate));

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(geminiResponse));

        String result = aiService.generateBulletPoints(1, 1, "Dev", "Company");
        assertEquals("- Bullet 1\n- Bullet 2", result);
    }

    @Test
    void improveSection_shouldReturnText() {
        setupWebClientMocking();

        Map<String, Object> geminiResponse = new HashMap<>();
        Map<String, Object> candidate = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", "{\"options\": [\"Improved text\"]}");
        content.put("parts", Arrays.asList(part));
        candidate.put("content", content);
        geminiResponse.put("candidates", Arrays.asList(candidate));

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(geminiResponse));

        String result = aiService.improveSection(1, "Text", "professional");
        assertEquals("{\"options\": [\"Improved text\"]}", result);
    }

    @Test
    void checkAtsCompatibility_shouldReturnReport() {
        setupWebClientMocking();

        String reportJson = "{\"score\": 80, \"verdict\": \"Good\", \"breakdown\": {\"keywordMatch\": {\"score\": 30, \"maxScore\": 35}}}";
        Map<String, Object> geminiResponse = new HashMap<>();
        Map<String, Object> candidate = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", reportJson);
        content.put("parts", Arrays.asList(part));
        candidate.put("content", content);
        geminiResponse.put("candidates", Arrays.asList(candidate));

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(geminiResponse));

        AtsReport result = aiService.checkAtsCompatibility(1, 1, "Resume", "JD");

        assertNotNull(result);
        assertEquals(80, result.getScore());
    }

    @Test
    void suggestSkills_shouldReturnList() {
        setupWebClientMocking();

        Map<String, Object> geminiResponse = new HashMap<>();
        Map<String, Object> candidate = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", "Java,Spring,Docker");
        content.put("parts", Arrays.asList(part));
        candidate.put("content", content);
        geminiResponse.put("candidates", Arrays.asList(candidate));

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(geminiResponse));

        List<String> result = aiService.suggestSkills("Developer");

        assertEquals(3, result.size());
        assertTrue(result.contains("Spring"));
    }

    @Test
    void translateResume_shouldReturnTranslatedText() {
        setupWebClientMocking();

        Map<String, Object> geminiResponse = new HashMap<>();
        Map<String, Object> candidate = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", "Translated Content");
        content.put("parts", Arrays.asList(part));
        candidate.put("content", content);
        geminiResponse.put("candidates", Arrays.asList(candidate));

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(geminiResponse));

        String result = aiService.translateResume(1, 1, "French");

        assertEquals("Translated Content", result);
    }

    @Test
    void cleanJsonResponse_shouldHandleMarkdown() {
        setupWebClientMocking();

        Map<String, Object> geminiResponse = new HashMap<>();
        Map<String, Object> candidate = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", "```json\n{\"options\": [\"Option\"]}\n```");
        content.put("parts", Arrays.asList(part));
        candidate.put("content", content);
        geminiResponse.put("candidates", Arrays.asList(candidate));

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(geminiResponse));

        String result = aiService.generateSummary(1, 1, "Dev", 5);
        assertEquals("{\"options\": [\"Option\"]}", result);
    }

    @Test
    void cleanJsonResponse_shouldHandleTruncation() {
        setupWebClientMocking();

        Map<String, Object> geminiResponse = new HashMap<>();
        Map<String, Object> candidate = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        // Truncated JSON after key
        part.put("text", "{\"options\": [\"Opt");
        content.put("parts", Arrays.asList(part));
        candidate.put("content", content);
        geminiResponse.put("candidates", Arrays.asList(candidate));

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(geminiResponse));

        String result = aiService.generateSummary(1, 1, "Dev", 5);
        // Should be balanced
        assertTrue(result.contains("}"));
    }

    @Test
    void getUserHistory_shouldReturnList() {
        when(repository.findByUserId(1)).thenReturn(Arrays.asList(new AiRequest()));
        List<AiRequest> result = aiService.getUserHistory(1);
        assertEquals(1, result.size());
    }
}
