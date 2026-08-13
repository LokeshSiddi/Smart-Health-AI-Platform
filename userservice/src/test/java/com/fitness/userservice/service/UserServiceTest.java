package com.fitness.userservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.userservice.dto.RegisterRequest;
import com.fitness.userservice.dto.UserResponse;
import com.fitness.userservice.exception.InvalidUserDataException;
import com.fitness.userservice.exception.UserNotFoundException;
import com.fitness.userservice.model.User;
import com.fitness.userservice.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UserService}.
 * Pure Mockito — no Spring context, no database.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    // ------------------------------------------------------------------
    // register()
    // ------------------------------------------------------------------

    @Test
    @DisplayName("register: happy path hashes password, persists once and maps the full response")
    void register_happyPath() {
        RegisterRequest request = validRequest();
        when(userRepository.existsByEmail("lokesh@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret1")).thenReturn("$2a$bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId("db-id-1");
            u.setCreatedAt(LocalDateTime.now());
            u.setUpdatedAt(LocalDateTime.now());
            return u;
        });

        UserResponse response = userService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(captor.capture());
        User persisted = captor.getValue();
        assertThat(persisted.getPassword()).isEqualTo("$2a$bcrypt-hash")
                .as("plaintext password must never reach the repository");
        assertThat(persisted.getKeycloakId()).isEqualTo("kc-123");
        assertThat(persisted.getEmail()).isEqualTo("lokesh@example.com");

        assertThat(response.getId()).isEqualTo("db-id-1");
        assertThat(response.getKeycloakId()).isEqualTo("kc-123");
        assertThat(response.getFirstName()).isEqualTo("Lokesh");
        assertThat(response.getLastName()).isEqualTo("Siddi");
        assertThat(response.getCreatedAt()).isNotNull();
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("register: existing email returns the existing user WITHOUT saving or encoding again")
    void register_existingEmail_returnsExistingUser() {
        RegisterRequest request = validRequest();
        User existing = persistedUser("db-id-9", "kc-999");

        when(userRepository.existsByEmail("lokesh@example.com")).thenReturn(true);
        when(userRepository.findByEmail("lokesh@example.com")).thenReturn(existing);

        UserResponse response = userService.register(request);

        assertThat(response.getId()).isEqualTo("db-id-9");
        assertThat(response.getKeycloakId()).isEqualTo("kc-999");
        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @ParameterizedTest(name = "register: rejects email [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void register_blankEmail_rejected(String email) {
        RegisterRequest request = validRequest();
        request.setEmail(email);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(InvalidUserDataException.class)
                .hasMessage("Email cannot be empty");

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("register: null password rejected")
    void register_nullPassword_rejected() {
        RegisterRequest request = validRequest();
        request.setPassword(null);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(InvalidUserDataException.class)
                .hasMessage("Password must be at least 6 characters");
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("register: 5-char password rejected, 6-char boundary accepted at controller layer")
    void register_shortPassword_rejected() {
        RegisterRequest request = validRequest();
        request.setPassword("12345");

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(InvalidUserDataException.class)
                .hasMessage("Password must be at least 6 characters");
        verifyNoInteractions(userRepository);
    }

    @ParameterizedTest(name = "register: rejects firstName [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  "})
    void register_blankFirstName_rejected(String firstName) {
        RegisterRequest request = validRequest();
        request.setFirstName(firstName);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(InvalidUserDataException.class)
                .hasMessage("First name cannot be empty");
        verifyNoInteractions(userRepository);
    }

    // ------------------------------------------------------------------
    // getUserProfile()
    // ------------------------------------------------------------------

    @Test
    @DisplayName("getUserProfile: resolves by KEYCLOAK id (not DB id) and maps response")
    void getUserProfile_found() {
        when(userRepository.findByKeycloakId("kc-123"))
                .thenReturn(Optional.of(persistedUser("db-id-1", "kc-123")));

        UserResponse response = userService.getUserProfile("kc-123");

        assertThat(response.getKeycloakId()).isEqualTo("kc-123");
        assertThat(response.getEmail()).isEqualTo("lokesh@example.com");
        verify(userRepository).findByKeycloakId("kc-123");
        verify(userRepository, never()).findById(anyString());
    }

    @Test
    @DisplayName("getUserProfile: unknown keycloak id throws UserNotFoundException with the id in the message")
    void getUserProfile_missing() {
        when(userRepository.findByKeycloakId("kc-404")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserProfile("kc-404"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("kc-404");
    }

    // ------------------------------------------------------------------
    // existByUserId()
    // ------------------------------------------------------------------

    @Test
    @DisplayName("existByUserId: passthrough of existsByKeycloakId (true and false)")
    void existByUserId_passthrough() {
        when(userRepository.existsByKeycloakId("kc-123")).thenReturn(true);
        when(userRepository.existsByKeycloakId("kc-404")).thenReturn(false);

        assertThat(userService.existByUserId("kc-123")).isTrue();
        assertThat(userService.existByUserId("kc-404")).isFalse();
    }

    @Test
    @DisplayName("existByUserId: blank id rejected before touching the repository")
    void existByUserId_blank() {
        assertThatThrownBy(() -> userService.existByUserId(" "))
                .isInstanceOf(InvalidUserDataException.class)
                .hasMessage("User ID cannot be empty");
        verifyNoInteractions(userRepository);
    }

    // ------------------------------------------------------------------
    // getUserResponse() — password hygiene
    // ------------------------------------------------------------------

    @Test
    @DisplayName("UserResponse must never serialize the password")
    void userResponse_neverExposesPassword() throws Exception {
        User user = persistedUser("db-id-1", "kc-123");
        user.setPassword("$2a$should-never-leak");

        UserResponse response = userService.getUserResponse(user);

        assertThat(Arrays.stream(UserResponse.class.getDeclaredFields()).map(Field::getName))
                .doesNotContain("password");
        String json = new ObjectMapper().writeValueAsString(response);
        assertThat(json).doesNotContain("password").doesNotContain("$2a$");
    }

    // ------------------------------------------------------------------
    // fixtures
    // ------------------------------------------------------------------

    private RegisterRequest validRequest() {
        RegisterRequest r = new RegisterRequest();
        r.setEmail("lokesh@example.com");
        r.setPassword("secret1");
        r.setKeycloakId("kc-123");
        r.setFirstName("Lokesh");
        r.setLastName("Siddi");
        return r;
    }

    private User persistedUser(String id, String keycloakId) {
        User u = new User();
        u.setId(id);
        u.setKeycloakId(keycloakId);
        u.setEmail("lokesh@example.com");
        u.setFirstName("Lokesh");
        u.setLastName("Siddi");
        u.setCreatedAt(LocalDateTime.now().minusDays(1));
        u.setUpdatedAt(LocalDateTime.now());
        return u;
    }
}
