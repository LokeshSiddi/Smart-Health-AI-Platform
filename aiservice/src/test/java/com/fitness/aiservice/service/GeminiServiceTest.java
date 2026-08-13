package com.fitness.aiservice.service;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GeminiService} against MockWebServer.
 *
 * IMPORTANT behavior under test: the .onErrorReturn(...) at the end of the
 * WebClient chain swallows EVERY error (400/429/404/5xx included) and returns
 * a synthetic "AI service temporarily unavailable" envelope. These tests lock
 * that in — and as a side effect, document that GeminiApiException can never
 * actually escape getAnswer() today.
 */
class GeminiServiceTest {

    private MockWebServer server;
    private GeminiService geminiService;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        geminiService = new GeminiService(WebClient.builder());
        ReflectionTestUtils.setField(geminiService, "geminiApiUrl",
                server.url("/v1beta/models/gemini-2.0-flash:generateContent?key=").toString());
        ReflectionTestUtils.setField(geminiService, "geminiApiKey", "TEST_KEY");
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    @DisplayName("200 → returns the raw body; request is a POST with key in URL, JSON content type and the prompt embedded")
    void happyPath() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"hello\"}]}}]}"));

        String response = geminiService.getAnswer("Analyse my run");

        assertThat(response).contains("\"hello\"");

        RecordedRequest request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).endsWith("TEST_KEY");
        assertThat(request.getHeader("Content-Type")).contains("application/json");
        assertThat(request.getBody().readUtf8())
                .contains("Analyse my run")
                .contains("contents")
                .contains("parts");
    }

    @ParameterizedTest(name = "HTTP {0} from Gemini is swallowed into the fallback envelope")
    @ValueSource(ints = {400, 429, 404, 500, 503})
    @DisplayName("error statuses never throw — the synthetic fallback envelope is returned instead")
    void errorStatuses_swallowedToFallback(int status) {
        server.enqueue(new MockResponse()
                .setResponseCode(status)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"error\":\"boom\"}"));

        String response = geminiService.getAnswer("Analyse my run");

        assertThat(response)
                .contains("candidates")
                .contains("AI service temporarily unavailable");
    }
}
