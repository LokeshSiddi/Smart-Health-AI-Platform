package com.fitness.gateway.user;

import com.fitness.gateway.dto.RegisterRequest;
import com.fitness.gateway.exception.InvalidUserDataException;
import com.fitness.gateway.exception.UserNotFoundException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reactive tests for the gateway-side {@link UserService} (WebClient passthrough
 * to the user-service) using StepVerifier + MockWebServer.
 */
class UserServiceTest {

    private MockWebServer server;
    private UserService userService;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        userService = new UserService(WebClient.create(server.url("/").toString()));
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    // ------------------------------------------------------------------
    // validateUser()
    // ------------------------------------------------------------------

    @Test
    @DisplayName("validateUser 200 → emits the boolean verdict")
    void validateUser_true() {
        server.enqueue(json(200, "true"));
        StepVerifier.create(userService.validateUser("kc-123"))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("validateUser 200 false → emits false")
    void validateUser_false() {
        server.enqueue(json(200, "false"));
        StepVerifier.create(userService.validateUser("kc-404"))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    @DisplayName("401 → RuntimeException 'Unauthorized :' (bad/expired token surfaced upstream)")
    void validateUser_unauthorized() {
        server.enqueue(json(401, ""));
        StepVerifier.create(userService.validateUser("kc-123"))
                .expectErrorMatches(t -> t instanceof RuntimeException
                        && t.getMessage().startsWith("Unauthorized : Invalid or missing token"))
                .verify();
    }

    @Test
    @DisplayName("404 → UserNotFoundException")
    void validateUser_notFound() {
        server.enqueue(json(404, ""));
        StepVerifier.create(userService.validateUser("kc-404"))
                .expectErrorMatches(t -> t instanceof UserNotFoundException
                        && t.getMessage().equals("User Not Found : kc-404"))
                .verify();
    }

    @Test
    @DisplayName("400 → InvalidUserDataException")
    void validateUser_badRequest() {
        server.enqueue(json(400, ""));
        StepVerifier.create(userService.validateUser("bad"))
                .expectErrorMatches(t -> t instanceof InvalidUserDataException
                        && t.getMessage().equals("Invalid Request : bad"))
                .verify();
    }

    @Test
    @DisplayName("unexpected status → generic RuntimeException 'Unexpected Error'")
    void validateUser_serverError() {
        server.enqueue(json(500, "{}"));
        StepVerifier.create(userService.validateUser("kc-123"))
                .expectErrorMatches(t -> t instanceof RuntimeException
                        && t.getMessage().startsWith("Unexpected Error"))
                .verify();
    }

    // ------------------------------------------------------------------
    // registerUser()
    // ------------------------------------------------------------------

    @Test
    @DisplayName("registerUser 200 → maps UserResponse and POSTs the RegisterRequest to /api/users/register")
    void registerUser_success() throws Exception {
        server.enqueue(json(200, """
                {"id":"db-1","keycloakId":"kc-123","email":"lokesh@example.com",
                 "firstName":"Lokesh","lastName":"Siddi"}
                """));

        StepVerifier.create(userService.registerUser(request()))
                .expectNextMatches(u -> "lokesh@example.com".equals(u.getEmail())
                        && "kc-123".equals(u.getKeycloakId()))
                .verifyComplete();

        RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("POST");
        assertThat(recorded.getPath()).isEqualTo("/api/users/register");
        assertThat(recorded.getBody().readUtf8()).contains("lokesh@example.com").contains("kc-123");
    }

    @Test
    @DisplayName("registerUser 400 → RuntimeException 'Bad Request'")
    void registerUser_badRequest() {
        server.enqueue(json(400, "{}"));
        StepVerifier.create(userService.registerUser(request()))
                .expectErrorMatches(t -> t instanceof RuntimeException
                        && t.getMessage().startsWith("Bad Request"))
                .verify();
    }

    @Test
    @DisplayName("registerUser 500 → RuntimeException 'Internal Server Error'")
    void registerUser_serverError() {
        server.enqueue(json(500, "{}"));
        StepVerifier.create(userService.registerUser(request()))
                .expectErrorMatches(t -> t instanceof RuntimeException
                        && t.getMessage().startsWith("Internal Server Error"))
                .verify();
    }

    private RegisterRequest request() {
        RegisterRequest r = new RegisterRequest();
        r.setEmail("lokesh@example.com");
        r.setPassword("password1");
        r.setKeycloakId("kc-123");
        r.setFirstName("Lokesh");
        r.setLastName("Siddi");
        return r;
    }

    private MockResponse json(int status, String body) {
        return new MockResponse()
                .setResponseCode(status)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(body);
    }
}
