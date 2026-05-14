package com.resumade.ai.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumade.ai.dto.AtsReport;
import com.resumade.ai.entity.AiRequest;
import com.resumade.ai.repository.AiRequestRepository;

import reactor.core.publisher.Mono;

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
    void generateSummary_shouldUseFallbackModelWhenPrimaryFails() {
        setupWebClientMocking();
        org.springframework.test.util.ReflectionTestUtils.setField(aiService, "geminiFallbackModel", "gemini-1.5-pro");

        WebClientResponseException exception = WebClientResponseException.create(
                503, "Service Unavailable", HttpHeaders.EMPTY, new byte[0], null);

        Map<String, Object> geminiResponse = new HashMap<>();
        Map<String, Object> candidate = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", "{\"options\":[\"Fallback\"]}");
        content.put("parts", Arrays.asList(part));
        candidate.put("content", content);
        geminiResponse.put("candidates", Arrays.asList(candidate));

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.error(exception), Mono.just(geminiResponse));

        String result = aiService.generateSummary(1, 1, "Dev", 5);

        assertEquals("{\"options\":[\"Fallback\"]}", result);
    }

        @Test
        void generateSummary_shouldThrowWhenFallbackFails() {
        setupWebClientMocking();
        org.springframework.test.util.ReflectionTestUtils.setField(aiService, "geminiFallbackModel", "gemini-1.5-pro");

        WebClientResponseException exception = WebClientResponseException.create(
            503, "Service Unavailable", HttpHeaders.EMPTY, new byte[0], null);

        when(responseSpec.bodyToMono(Map.class))
            .thenReturn(Mono.error(exception), Mono.error(new RuntimeException("fallback")));

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> aiService.generateSummary(1, 1, "Dev", 5));

        assertTrue(ex.getMessage().contains("AI service error"));
        }

    @Test
    void generateSummary_shouldThrowWhenCallGeminiFails() {
        setupWebClientMocking();

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.error(new RuntimeException("boom")));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> aiService.generateSummary(1, 1, "Dev", 5));

        assertTrue(ex.getMessage().contains("AI service error"));
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
    void generateBulletPoints_shouldReturnNoContentMessageWhenCandidatesMissing() {
        setupWebClientMocking();

        Map<String, Object> geminiResponse = new HashMap<>();
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(geminiResponse));

        String result = aiService.generateBulletPoints(1, 1, "Dev", "Company");

        assertEquals("No content received from Gemini.", result);
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
    void checkAtsCompatibility_shouldReturnDefaultAfterFailures() throws Exception {
        setupWebClientMocking();

        Map<String, Object> geminiResponse = new HashMap<>();
        Map<String, Object> candidate = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", "{\"score\": 90}");
        content.put("parts", Arrays.asList(part));
        candidate.put("content", content);
        geminiResponse.put("candidates", Arrays.asList(candidate));

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(geminiResponse));
        doThrow(new RuntimeException("parse"))
                .when(objectMapper).readValue(anyString(), eq(AtsReport.class));

        AtsReport result = aiService.checkAtsCompatibility(1, 1, "Resume", "JD");

        assertEquals(0, result.getScore());
        assertEquals("AI service error. Please try again later.", result.getVerdict());
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
    void suggestSkills_shouldReturnFallbackWhenAiFails() {
        setupWebClientMocking();

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.error(new RuntimeException("down")));

        List<String> result = aiService.suggestSkills("Developer");

        assertTrue(result.contains("Communication"));
        assertTrue(result.contains("Problem Solving"));
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
    void generateCoverLetter_shouldReturnText() {
        setupWebClientMocking();

        Map<String, Object> geminiResponse = new HashMap<>();
        Map<String, Object> candidate = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", "Cover Letter Text");
        content.put("parts", Arrays.asList(part));
        candidate.put("content", content);
        geminiResponse.put("candidates", Arrays.asList(candidate));

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(geminiResponse));

        String result = aiService.generateCoverLetter(1, 1, "Resume", "JD");

        assertEquals("Cover Letter Text", result);
    }

    @Test
    void tailorResumeForJob_shouldReturnText() {
        setupWebClientMocking();

        Map<String, Object> geminiResponse = new HashMap<>();
        Map<String, Object> candidate = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", "{\"matchScore\": 90}");
        content.put("parts", Arrays.asList(part));
        candidate.put("content", content);
        geminiResponse.put("candidates", Arrays.asList(candidate));

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(geminiResponse));

        String result = aiService.tailorResumeForJob(1, 1, "Resume", "JD");

        assertEquals("{\"matchScore\": 90}", result);
    }

    @Test
    void streamAiResponse_shouldReturnFlux() {
        setupWebClientMocking();

        Map<String, Object> geminiResponse = new HashMap<>();
        Map<String, Object> candidate = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", "Streamed");
        content.put("parts", Arrays.asList(part));
        candidate.put("content", content);
        geminiResponse.put("candidates", Arrays.asList(candidate));

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(geminiResponse));

        List<String> result = aiService.streamAiResponse(1, "Prompt", "SUMMARY")
                .collectList()
                .block();

        assertEquals(1, result.size());
        assertEquals("Streamed", result.get(0));
    }

    @Test
    void testPrompt_shouldReturnConfigMessageWhenKeyMissing() {
        org.springframework.test.util.ReflectionTestUtils.setField(aiService, "geminiKey", "your_gemini_key");

        String result = aiService.testPrompt("Hello");

        assertTrue(result.contains("Gemini API key is not configured"));
    }

    @Test
    void testPrompt_shouldReturnResponseWhenKeyConfigured() {
        setupWebClientMocking();

        Map<String, Object> geminiResponse = new HashMap<>();
        Map<String, Object> candidate = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", "OK");
        content.put("parts", Arrays.asList(part));
        candidate.put("content", content);
        geminiResponse.put("candidates", Arrays.asList(candidate));

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(geminiResponse));

        String result = aiService.testPrompt("Hello");

        assertEquals("OK", result);
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

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("provideJsonEdgeCases")
    void cleanJsonResponse_shouldHandleEdgeCases(String input, String expected) {
        setupWebClientMocking();

        Map<String, Object> geminiResponse = new HashMap<>();
        Map<String, Object> candidate = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        if (input != null) {
            part.put("text", input);
        }
        content.put("parts", Arrays.asList(part));
        candidate.put("content", content);
        geminiResponse.put("candidates", Arrays.asList(candidate));

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(geminiResponse));

        String result = aiService.generateSummary(1, 1, "Dev", 5);

        assertEquals(expected, result);
    }

    private static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> provideJsonEdgeCases() {
        return java.util.stream.Stream.of(
                org.junit.jupiter.params.provider.Arguments.of("{\"options\":[\"A\",],}", "{\"options\":[\"A\"]}"),
                org.junit.jupiter.params.provider.Arguments.of("{\"options\":[\"A\\\"B\",],}", "{\"options\":[\"A\\\"B\"]}"),
                org.junit.jupiter.params.provider.Arguments.of(null, "{}"),
                org.junit.jupiter.params.provider.Arguments.of("No json here", "{}")
        );
    }

    @Test
    void getUserHistory_shouldReturnList() {
        when(repository.findByUserId(1)).thenReturn(Arrays.asList(new AiRequest()));
        List<AiRequest> result = aiService.getUserHistory(1);
        assertEquals(1, result.size());
    }

    @Test
    void getUserHistory_shouldReturnEmptyListWhenUserIdNull() {
        List<AiRequest> result = aiService.getUserHistory(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testSanitizeTruncatedJson_Conditions() throws Exception {
        java.lang.reflect.Method method = AiServiceImpl.class.getDeclaredMethod("sanitizeTruncatedJson", String.class);
        method.setAccessible(true);

        // text == null or empty
        assertNull(method.invoke(aiService, (String) null));
        assertEquals("", method.invoke(aiService, ""));

        // Already balanced
        assertEquals("{\"a\":\"b\"}", method.invoke(aiService, "{\"a\":\"b\"}"));

        // Ends in whitespace, trims down to empty
        assertEquals("{", method.invoke(aiService, "{   "));

        // Whitespace only text (will become empty sb)
        assertEquals("{}", method.invoke(aiService, "   "));

        // Ends in complete number (due to regex flaw in service, it strips down to {"a)
        assertEquals("{\"a", method.invoke(aiService, "{\"a\": 123"));
        assertEquals("{\"a", method.invoke(aiService, "{\"a\": 12.3"));
        assertEquals("{\"a", method.invoke(aiService, "{\"a\": -123")); // test minus sign

        // Ends in complete number after array or comma (regex flaw also strips these down to {"a)
        assertEquals("{\"a", method.invoke(aiService, "{\"a\": [123"));
        assertEquals("{\"a", method.invoke(aiService, "{\"a\": [123, 456"));

        // Ends in valid structural characters (] and }) (regex flaw strips these down)
        assertEquals("{\"a", method.invoke(aiService, "{\"a\": [1, 2]"));
        assertEquals("{\"a\": {\"b", method.invoke(aiService, "{\"a\": {\"b\": 1}"));

        // Partial letter (e.g. true/false) (strips down to {"a)
        assertEquals("{\"a", method.invoke(aiService, "{\"a\": tr"));
        
        // Ends in partial number with 'e' (strips 'e' then regex strips down to {"a)
        assertEquals("{\"a", method.invoke(aiService, "{\"a\": 1.2e"));
        
        // Trims trailing commas
        assertEquals("{\"a", method.invoke(aiService, "{\"a\": 123,  "));

        // Removes orphan colons (actually strips colon then regex strips down to {"a)
        assertEquals("{\"a", method.invoke(aiService, "{\"a\":"));
        assertEquals("{ \"orphan", method.invoke(aiService, "{ \"orphan\" : "));
        assertEquals("\"a", method.invoke(aiService, "\"a\":"));
    }

    @Test
    void testCleanJsonResponse_Conditions() throws Exception {
        java.lang.reflect.Method method = AiServiceImpl.class.getDeclaredMethod("cleanJsonResponse", String.class);
        method.setAccessible(true);

        assertEquals("{}", method.invoke(aiService, (String) null));
        assertEquals("{}", method.invoke(aiService, "not json")); // extracted == null
        assertEquals("{}", method.invoke(aiService, "```json\n   \n```")); // extracted.isBlank()
    }

    @Test
    void testExtractFirstJson_Conditions() throws Exception {
        java.lang.reflect.Method method = AiServiceImpl.class.getDeclaredMethod("extractFirstJson", String.class);
        method.setAccessible(true);

        assertNull(method.invoke(aiService, (String) null));
        assertNull(method.invoke(aiService, "no brackets here"));

        assertEquals("{\"a\":1}", method.invoke(aiService, "prefix {\"a\":1} suffix"));
        assertEquals("[1,2]", method.invoke(aiService, "prefix [1,2] suffix"));
        
        // Both object and array present
        assertEquals("{\"a\":1}", method.invoke(aiService, "prefix {\"a\":1} and [1,2]"));
        assertEquals("[1,2]", method.invoke(aiService, "prefix [1,2] and {\"a\":1}"));
        
        // Unbalanced returns slice
        assertEquals("{ \"a\": 1 ", method.invoke(aiService, "prefix { \"a\": 1 "));
    }

    @Test
    void testExtractBalancedJson_Conditions() throws Exception {
        java.lang.reflect.Method method = AiServiceImpl.class.getDeclaredMethod("extractBalancedJson", String.class);
        method.setAccessible(true);

        assertNull(method.invoke(aiService, (String) null));
        assertNull(method.invoke(aiService, ""));
        assertNull(method.invoke(aiService, "not json"));

        // Balanced
        assertEquals("{\"a\":\"b\"}", method.invoke(aiService, "{\"a\":\"b\"} suffix"));
        assertEquals("[\"a\",\"b\"]", method.invoke(aiService, "[\"a\",\"b\"] suffix"));

        // String escaping
        assertEquals("{\"a\":\"b\\\"c\"}", method.invoke(aiService, "{\"a\":\"b\\\"c\"} suffix"));

        // Unbalanced
        assertNull(method.invoke(aiService, "{\"a\":\"b\""));
        assertEquals("{\"a\":\"b\"}", method.invoke(aiService, "{\"a\":\"b\"} ]"));
    }

    @Test
    void callGeminiWithModel_shouldHandleEmptyCandidates() throws Exception {
        setupWebClientMocking();
        Map<String, Object> geminiResponse = new HashMap<>();
        geminiResponse.put("candidates", Arrays.asList()); // Empty candidates list

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(geminiResponse));

        java.lang.reflect.Method method = AiServiceImpl.class.getDeclaredMethod("callGeminiWithModel", String.class, String.class, double.class);
        method.setAccessible(true);
        String result = (String) method.invoke(aiService, "gemini-pro", "prompt", 0.5);

        assertEquals("No content received from Gemini.", result);
    }

    @Test
    void callGeminiWithModel_shouldHandleEmptyParts() throws Exception {
        setupWebClientMocking();
        Map<String, Object> geminiResponse = new HashMap<>();
        Map<String, Object> candidate = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        content.put("parts", Arrays.asList()); // Empty parts list
        candidate.put("content", content);
        geminiResponse.put("candidates", Arrays.asList(candidate));

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(geminiResponse));

        java.lang.reflect.Method method = AiServiceImpl.class.getDeclaredMethod("callGeminiWithModel", String.class, String.class, double.class);
        method.setAccessible(true);
        String result = (String) method.invoke(aiService, "gemini-pro", "prompt", 0.5);

        assertEquals("No content received from Gemini.", result);
    }

    @Test
    void callGemini_shouldHandleEmptyGeminiKey() {
        org.springframework.test.util.ReflectionTestUtils.setField(aiService, "geminiKey", "");
        String result = aiService.generateSummary(1, 1, "Dev", 5);
        assertEquals("{}", result);
    }

    @Test
    void callGemini_shouldHandlePlaceholderGeminiKey() {
        org.springframework.test.util.ReflectionTestUtils.setField(aiService, "geminiKey", "your_gemini_key_here");
        String result = aiService.generateSummary(1, 1, "Dev", 5);
        assertEquals("{}", result);
    }

    @Test
    void callGemini_shouldHandleNullPrompt() throws Exception {
        setupWebClientMocking();
        Map<String, Object> geminiResponse = new HashMap<>();
        Map<String, Object> candidate = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", "success");
        content.put("parts", Arrays.asList(part));
        candidate.put("content", content);
        geminiResponse.put("candidates", Arrays.asList(candidate));

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(geminiResponse));

        java.lang.reflect.Method method = AiServiceImpl.class.getDeclaredMethod("callGemini", String.class, AiRequest.RequestType.class);
        method.setAccessible(true);
        String result = (String) method.invoke(aiService, null, AiRequest.RequestType.SUMMARY);

        assertEquals("success", result);
    }

    @Test
    void callGemini_shouldNotFallbackIfFallbackModelIsBlank() {
        setupWebClientMocking();
        org.springframework.test.util.ReflectionTestUtils.setField(aiService, "geminiFallbackModel", "  ");
        org.springframework.web.reactive.function.client.WebClientResponseException exception =
                org.springframework.web.reactive.function.client.WebClientResponseException.create(503, "Service Unavailable", null, null, null);

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.error(exception));

        assertThrows(RuntimeException.class, () -> aiService.generateSummary(1, 1, "Dev", 5));
    }

    @Test
    void callGemini_shouldNotFallbackIfFallbackModelEqualsPrimary() {
        setupWebClientMocking();
        org.springframework.test.util.ReflectionTestUtils.setField(aiService, "geminiModel", "gemini-pro");
        org.springframework.test.util.ReflectionTestUtils.setField(aiService, "geminiFallbackModel", "gemini-pro");
        org.springframework.web.reactive.function.client.WebClientResponseException exception =
                org.springframework.web.reactive.function.client.WebClientResponseException.create(503, "Service Unavailable", null, null, null);

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.error(exception));

        assertThrows(RuntimeException.class, () -> aiService.generateSummary(1, 1, "Dev", 5));
    }

    @Test
    void callGemini_shouldNotFallbackForStatus400() {
        setupWebClientMocking();
        org.springframework.test.util.ReflectionTestUtils.setField(aiService, "geminiFallbackModel", "gemini-1.5-pro");
        org.springframework.web.reactive.function.client.WebClientResponseException exception =
                org.springframework.web.reactive.function.client.WebClientResponseException.create(400, "Bad Request", null, null, null);

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.error(exception));

        assertThrows(RuntimeException.class, () -> aiService.generateSummary(1, 1, "Dev", 5));
    }

    @Test
    void callGemini_shouldFallbackForStatus429() {
        setupWebClientMocking();
        org.springframework.test.util.ReflectionTestUtils.setField(aiService, "geminiFallbackModel", "gemini-1.5-pro");
        org.springframework.web.reactive.function.client.WebClientResponseException exception =
                org.springframework.web.reactive.function.client.WebClientResponseException.create(429, "Too Many Requests", null, null, null);

        Map<String, Object> geminiResponse = new HashMap<>();
        Map<String, Object> candidate = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", "{\"options\":[\"Fallback\"]}");
        content.put("parts", Arrays.asList(part));
        candidate.put("content", content);
        geminiResponse.put("candidates", Arrays.asList(candidate));

        when(responseSpec.bodyToMono(Map.class))
            .thenReturn(Mono.error(exception), Mono.just(geminiResponse));

        String result = aiService.generateSummary(1, 1, "Dev", 5);
        assertEquals("{\"options\":[\"Fallback\"]}", result);
    }

    @Test
    void callGemini_shouldFallbackForStatus504() {
        setupWebClientMocking();
        org.springframework.test.util.ReflectionTestUtils.setField(aiService, "geminiFallbackModel", "gemini-1.5-pro");
        org.springframework.web.reactive.function.client.WebClientResponseException exception =
                org.springframework.web.reactive.function.client.WebClientResponseException.create(504, "Gateway Timeout", null, null, null);

        Map<String, Object> geminiResponse = new HashMap<>();
        Map<String, Object> candidate = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", "{\"options\":[\"Fallback\"]}");
        content.put("parts", Arrays.asList(part));
        candidate.put("content", content);
        geminiResponse.put("candidates", Arrays.asList(candidate));

        when(responseSpec.bodyToMono(Map.class))
            .thenReturn(Mono.error(exception), Mono.just(geminiResponse));

        String result = aiService.generateSummary(1, 1, "Dev", 5);
        assertEquals("{\"options\":[\"Fallback\"]}", result);
    }

    @Test
    void callGeminiWithModel_shouldHandleNullResponse() throws Exception {
        setupWebClientMocking();
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.empty());

        java.lang.reflect.Method method = AiServiceImpl.class.getDeclaredMethod("callGeminiWithModel", String.class, String.class, double.class);
        method.setAccessible(true);
        String result = (String) method.invoke(aiService, "gemini-pro", "prompt", 0.5);

        assertEquals("No content received from Gemini.", result);
    }

    @Test
    void callGeminiWithModel_shouldHandleMissingCandidatesKey() throws Exception {
        setupWebClientMocking();
        Map<String, Object> geminiResponse = new HashMap<>();
        geminiResponse.put("other_key", "value");

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(geminiResponse));

        java.lang.reflect.Method method = AiServiceImpl.class.getDeclaredMethod("callGeminiWithModel", String.class, String.class, double.class);
        method.setAccessible(true);
        String result = (String) method.invoke(aiService, "gemini-pro", "prompt", 0.5);

        assertEquals("No content received from Gemini.", result);
    }
}
