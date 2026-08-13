package com.fitness.gateway.filter;

import com.fitness.gateway.dto.RegisterRequest;
import com.fitness.gateway.dto.UserResponse;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link KeycloakUserSyncFilter}.
 * Real HS256-signed JWTs are minted with Nimbus (available because the gateway
 * ships spring-boot-starter-oauth2-resource-server) so the parsing path is
 * exercised for real; UserService is mocked.
 */
@ExtendWith(MockitoExtension.class)
class KeycloakUserSyncFilterTest {

    @Mock
    private com.fitness.gateway.user.UserService userService;

    @InjectMocks
    private KeycloakUserSyncFilter filter;

    @Test
    @DisplayName("/actuator paths bypass sync entirely")
    void actuatorPath_skips() {
        CapturingChain chain = new CapturingChain();
        filter.filter(exchange(MockServerHttpRequest.get("/actuator/health")), chain).block();

        assertThat(chain.invoked()).isTrue();
        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("request without an Authorization header bypasses sync")
    void noAuthHeader_skips() {
        CapturingChain chain = new CapturingChain();
        filter.filter(exchange(MockServerHttpRequest.get("/api/activities")), chain).block();

        assertThat(chain.invoked()).isTrue();
        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("unparseable Bearer token bypasses sync (getUserDetails returns null)")
    void malformedToken_skips() {
        CapturingChain chain = new CapturingChain();
        filter.filter(exchange(MockServerHttpRequest.get("/api/activities")
                .header("Authorization", "Bearer definitely-not-a-jwt")), chain).block();

        assertThat(chain.invoked()).isTrue();
        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("existing user: validate only (no register), X-User-ID = keycloak sub header is injected downstream")
    void existingUser_validatesAndMutatesHeader() throws Exception {
        when(userService.validateUser("sub-123")).thenReturn(Mono.just(true));

        CapturingChain chain = new CapturingChain();
        filter.filter(exchange(MockServerHttpRequest.get("/api/activities")
                .header("Authorization", "Bearer " + signedJwt())), chain).block();

        assertThat(chain.invoked()).isTrue();
        verify(userService).validateUser("sub-123");
        verify(userService, never()).registerUser(any());
        assertThat(chain.headerOf("X-User-ID")).isEqualTo("sub-123");
    }

    @Test
    @DisplayName("new user: registers from JWT claims (sub→keycloakId, given_name/family_name→names), then continues")
    void newUser_registers() throws Exception {
        when(userService.validateUser(anyString())).thenReturn(Mono.just(false));
        when(userService.registerUser(any(RegisterRequest.class)))
                .thenReturn(Mono.just(new UserResponse()));

        CapturingChain chain = new CapturingChain();
        filter.filter(exchange(MockServerHttpRequest.get("/api/activities")
                .header("Authorization", "Bearer " + signedJwt())), chain).block();

        ArgumentCaptor<RegisterRequest> captor = ArgumentCaptor.forClass(RegisterRequest.class);
        verify(userService).registerUser(captor.capture());
        RegisterRequest sent = captor.getValue();
        assertThat(sent.getKeycloakId()).isEqualTo("sub-123");
        assertThat(sent.getEmail()).isEqualTo("lokesh@example.com");
        assertThat(sent.getFirstName()).isEqualTo("Lokesh");
        assertThat(sent.getLastName()).isEqualTo("Siddi");
        assertThat(sent.getPassword()).isEqualTo("password1"); // placeholder, set by the filter

        assertThat(chain.headerOf("X-User-ID")).isEqualTo("sub-123");
    }

    @Test
    @DisplayName("an existing X-User-ID header wins over the token's sub claim")
    void explicitUserIdHeader_wins() throws Exception {
        when(userService.validateUser("explicit-user")).thenReturn(Mono.just(true));

        CapturingChain chain = new CapturingChain();
        filter.filter(exchange(MockServerHttpRequest.get("/api/activities")
                .header("Authorization", "Bearer " + signedJwt())
                .header("X-User-ID", "explicit-user")), chain).block();

        verify(userService).validateUser("explicit-user");
        assertThat(chain.headerOf("X-User-ID")).isEqualTo("explicit-user");
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private static MockServerWebExchange exchange(MockServerHttpRequest.BaseBuilder<?> builder) {
        return MockServerWebExchange.from(builder);
    }

    private static String signedJwt() throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("sub-123")                     // → keycloakId
                .claim("email", "lokesh@example.com")
                .claim("given_name", "Lokesh")          // → firstName
                .claim("family_name", "Siddi")          // → lastName
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner("0123456789-abcdef-0123456789-abcdef".getBytes(StandardCharsets.UTF_8)));
        return jwt.serialize();
    }

    /** A WebFilterChain that records the (possibly mutated) exchange it receives. */
    private static final class CapturingChain implements WebFilterChain {
        private final AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

        @Override
        public Mono<Void> filter(ServerWebExchange exchange) {
            captured.set(exchange);
            return Mono.empty();
        }

        boolean invoked() {
            return captured.get() != null;
        }

        String headerOf(String name) {
            return captured.get().getRequest().getHeaders().getFirst(name);
        }
    }
}
