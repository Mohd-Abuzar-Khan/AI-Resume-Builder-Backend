package com.resumade.jobmatch.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumade.jobmatch.entity.JobMatch;
import com.resumade.jobmatch.repository.JobMatchRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobMatchServiceImplTest {

    @Mock
    private JobMatchRepository repository;
    @Mock
    private WebClient.Builder webClientBuilder;
    @Mock
    private WebClient.Builder loadBalancedWebClientBuilder;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

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

    @InjectMocks
    private JobMatchServiceImpl jobMatchService;

    private JobMatch mockJobMatch;

    @BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(jobMatchService, "webClientBuilder",
                webClientBuilder);
        org.springframework.test.util.ReflectionTestUtils.setField(jobMatchService, "loadBalancedWebClientBuilder",
                loadBalancedWebClientBuilder);
        org.springframework.test.util.ReflectionTestUtils.setField(jobMatchService, "joobleApiKey", "test-key");
        mockJobMatch = new JobMatch();
        mockJobMatch.setMatchId(1L);
        mockJobMatch.setJobTitle("Java");
        mockJobMatch.setJobDescription("Java Developer");
    }

    private void setupWebClientMocking() {
        lenient().when(webClientBuilder.build()).thenReturn(webClient);
        lenient().when(webClient.post()).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void searchJobs_shouldReturnFromJooble() {
        setupWebClientMocking();

        Map<String, Object> responseMap = new HashMap<>();
        Map<String, Object> job = new HashMap<>();
        job.put("title", "Java Dev");
        job.put("company", "Company");
        job.put("location", "NY");
        job.put("snippet", "Dev snippet &nbsp;");
        job.put("link", "url");
        responseMap.put("jobs", Arrays.asList(job));

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(responseMap));
        when(repository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<JobMatch> result = jobMatchService.searchJobs(1, "Java", "NY", "in", 1);

        assertEquals(1, result.size());
        assertEquals("Java Dev", result.get(0).getJobTitle());
    }

    @Test
    void analyzeJobFit_shouldReturnAnalyzedJob() {
        setupWebClientMocking();
        when(repository.findById(1L)).thenReturn(Optional.of(mockJobMatch));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Mock Resume Service Call
        Map<String, Object> resumeData = new HashMap<>();
        resumeData.put("title", "My Resume");
        resumeData.put("sections",
                Arrays.asList(Map.of("sectionType", "EXPERIENCE", "content", "Worked at Java Corp")));

        WebClient.RequestHeadersUriSpec getUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec getHeadersSpec = mock(WebClient.RequestHeadersSpec.class);

        when(loadBalancedWebClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(getUriSpec);
        when(getUriSpec.uri(anyString())).thenReturn(getHeadersSpec);
        when(getHeadersSpec.header(anyString(), anyString())).thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(responseSpec);

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(resumeData));

        // Mock Gemini Response
        Map<String, Object> geminiResponse = new HashMap<>();
        Map<String, Object> candidate = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text",
                "{\"score\": 85, \"strengths\": \"Good Java\", \"weaknesses\": \"No SQL\", \"recommendations\": \"Learn SQL\"}");
        content.put("parts", Arrays.asList(part));
        candidate.put("content", content);
        geminiResponse.put("candidates", Arrays.asList(candidate));

        // We need to return geminiResponse when calling Gemini API via WebClient POST
        // This is tricky because setupWebClientMocking is already set for Jooble.
        // But analyzeJobFit calls Gemini AFTER fetching resume.
        // We can use thenAnswer to return different responses based on URI if needed,
        // or just return Gemini response for the second POST call.
        when(responseSpec.bodyToMono(Map.class))
                .thenReturn(Mono.just(resumeData)) // First call (GET resume)
                .thenReturn(Mono.just(geminiResponse)); // Second call (POST gemini)

        JobMatch result = jobMatchService.analyzeJobFit(1, 1, 1L, "token");

        assertNotNull(result);
        assertEquals(85, result.getMatchScore());
        assertEquals("Good Java", result.getStrengths());
    }

    @Test
    void testJooble_shouldReturnResponse() {
        setupWebClientMocking();
        Map<String, Object> response = Map.of("status", "ok");
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(response));

        Map<String, Object> result = jobMatchService.testJooble("Java", "NY");

        assertEquals("ok", result.get("status"));
    }

    @Test
    void toggleBookmark_shouldToggle() {
        mockJobMatch.setBookmarked(false);
        when(repository.findById(1L)).thenReturn(Optional.of(mockJobMatch));

        jobMatchService.toggleBookmark(1L);

        assertTrue(mockJobMatch.isBookmarked());
        verify(repository).save(mockJobMatch);
    }

    @Test
    void getUserHistory_shouldReturnList() {
        when(repository.findByUserIdOrderByMatchedAtDesc(1)).thenReturn(Arrays.asList(mockJobMatch));
        List<JobMatch> result = jobMatchService.getUserHistory(1);
        assertEquals(1, result.size());
    }

    @Test
    void getBookmarks_shouldReturnList() {
        when(repository.findByUserIdAndIsBookmarkedTrue(1)).thenReturn(Arrays.asList(mockJobMatch));
        List<JobMatch> result = jobMatchService.getBookmarks(1);
        assertEquals(1, result.size());
    }

    @Test
    void fetchJobsFallback_shouldReturnEmpty() {
        List<JobMatch> result = jobMatchService.fetchJobsFallback(1, "Java", "NY", new RuntimeException("err"));
        assertTrue(result.isEmpty());
    }
}
