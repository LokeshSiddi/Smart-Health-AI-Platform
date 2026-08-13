package com.fitness.activityservice.service;

import com.fitness.activityservice.exception.UserValidationException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link UserValidationService} against a real HTTP server (MockWebServer)
 * instead of mocking the WebClient fluent chain — verifies status mapping, body
 * handling and the exact outbound URI.
 */
class UserValidationServiceTest {

    private MockWebServer server;
    private UserValidationService service;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        // In production the bean is @LoadBalanced with baseUrl http://USER-SERVICE
        service = new UserValidationService(WebClient.create(server.url("/").toString()));
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    @DisplayName("200 'true' → true, and the request hits /api/users/{id}/validate")
    void validUser_true() throws Exception {
        server.enqueue(json(200, "true"));

        assertThat(service.validateUser("kc-123")).isTrue();

        RecordedRequest request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/api/users/kc-123/validate");
    }

    @Test
    @DisplayName("200 'false' → false")
    void userServiceSaysFalse() {
        server.enqueue(json(200, "false"));
        assertThat(service.validateUser("kc-404")).isFalse();
    }

    @Test
    @DisplayName("200 with empty body → false (null-safety of Boolean.TRUE.equals)")
    void emptyBody_meansFalse() {
        server.enqueue(json(200, ""));
        assertThat(service.validateUser("kc-1")).isFalse();
    }

    @Test
    @DisplayName("404 → UserValidationException 'User not found with ID: ...'")
    void notFound_throws() {
        server.enqueue(json(404, "{}"));

        assertThatThrownBy(() -> service.validateUser("kc-404"))
                .isInstanceOf(UserValidationException.class)
                .hasMessage("User not found with ID: kc-404");
    }

    @Test
    @DisplayName("400 → UserValidationException 'Invalid Request : ...'")
    void badRequest_throws() {
        server.enqueue(json(400, "{}"));

        assertThatThrownBy(() -> service.validateUser("bad"))
                .isInstanceOf(UserValidationException.class)
                .hasMessage("Invalid Request : bad");
    }

    @Test
    @DisplayName("500 from user-service is swallowed into 'false' — no exception escapes")
    void serverError_returnsFalse() {
        server.enqueue(json(500, "{}"));
        assertThat(service.validateUser("kc-1")).isFalse();
    }

    private MockResponse json(int status, String body) {
        return new MockResponse()
                .setResponseCode(status)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(body);
    }
}
