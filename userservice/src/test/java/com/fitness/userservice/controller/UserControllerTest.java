package com.fitness.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.userservice.dto.RegisterRequest;
import com.fitness.userservice.dto.UserResponse;
import com.fitness.userservice.exception.InvalidUserDataException;
import com.fitness.userservice.exception.UserNotFoundException;
import com.fitness.userservice.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web layer slice for {@link UserController} — exercises routing, JSON binding,
 * @Valid enforcement and the GlobalExceptionHandler ErrorResponse contract.
 * Note: @MockBean works on all Boot 3.x lines; if you are on Boot 3.4+ you may
 * switch to @MockitoBean to silence the deprecation warning.
 */
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false) // only relevant if spring-security-web lands on the classpath
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    @DisplayName("POST /api/users/register returns 200 and the mapped user JSON")
    void register_success() throws Exception {
        when(userService.register(any(RegisterRequest.class))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("db-id-1"))
                .andExpect(jsonPath("$.keycloakId").value("kc-123"))
                .andExpect(jsonPath("$.email").value("lokesh@example.com"))
                .andExpect(jsonPath("$.firstName").value("Lokesh"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/users/register with a 5-char password → 400 + validationErrors.password")
    void register_beanValidationFailure() throws Exception {
        RegisterRequest invalid = validRequest();
        invalid.setPassword("12345");

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.password").value("Password must have atleast 6 characters"))
                .andExpect(jsonPath("$.path").value("/api/users/register"));
    }

    @Test
    @DisplayName("POST /api/users/register with blank firstName → 400 + validationErrors.firstName")
    void register_blankFirstName() throws Exception {
        RegisterRequest invalid = validRequest();
        invalid.setFirstName("  ");

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.firstName").value("First name is required"));
    }

    @Test
    @DisplayName("service-level InvalidUserDataException maps to 400 BAD_REQUEST ErrorResponse")
    void register_invalidUserDataException() throws Exception {
        when(userService.register(any())).thenThrow(new InvalidUserDataException("Email cannot be empty"));

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Email cannot be empty"));
    }

    @Test
    @DisplayName("GET /api/users/{userId} → 404 ErrorResponse when profile missing")
    void getProfile_notFound() throws Exception {
        when(userService.getUserProfile("kc-404"))
                .thenThrow(new UserNotFoundException("User not found with ID: kc-404"));

        mockMvc.perform(get("/api/users/kc-404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User not found with ID: kc-404"))
                .andExpect(jsonPath("$.path").value("/api/users/kc-404"));
    }

    @Test
    @DisplayName("GET /api/users/{userId}/validate returns the boolean verdict")
    void validateUser() throws Exception {
        when(userService.existByUserId("kc-123")).thenReturn(true);
        when(userService.existByUserId("kc-404")).thenReturn(false);

        mockMvc.perform(get("/api/users/kc-123/validate"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        mockMvc.perform(get("/api/users/kc-404/validate"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    private RegisterRequest validRequest() {
        RegisterRequest r = new RegisterRequest();
        r.setEmail("lokesh@example.com");
        r.setPassword("secret1");
        r.setKeycloakId("kc-123");
        r.setFirstName("Lokesh");
        r.setLastName("Siddi");
        return r;
    }

    private UserResponse sampleResponse() {
        UserResponse u = new UserResponse();
        u.setId("db-id-1");
        u.setKeycloakId("kc-123");
        u.setEmail("lokesh@example.com");
        u.setFirstName("Lokesh");
        u.setLastName("Siddi");
        u.setCreatedAt(LocalDateTime.now());
        u.setUpdatedAt(LocalDateTime.now());
        return u;
    }
}
