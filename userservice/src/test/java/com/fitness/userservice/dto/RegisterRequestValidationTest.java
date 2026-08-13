package com.fitness.userservice.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bean-Validation contract for {@link RegisterRequest} — plain Jakarta Validator,
 * no Spring context. Locks in the annotations the @Valid controller relies on.
 */
class RegisterRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void init() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void close() {
        factory.close();
    }

    @Test
    @DisplayName("fully populated request passes validation")
    void validRequestPasses() {
        assertThat(validator.validate(valid())).isEmpty();
    }

    @Test
    @DisplayName("keycloakId is optional — null produces no violation")
    void keycloakIdOptional() {
        RegisterRequest r = valid();
        r.setKeycloakId(null);
        assertThat(validator.validate(r)).isEmpty();
    }

    @Test
    @DisplayName("blank email fails with 'Email is required'")
    void blankEmail() {
        RegisterRequest r = valid();
        r.setEmail("");
        assertThat(messagesOf(r)).contains("Email is required");
    }

    @Test
    @DisplayName("malformed email fails with 'Invalid Email Format'")
    void malformedEmail() {
        RegisterRequest r = valid();
        r.setEmail("not-an-email");
        assertThat(messagesOf(r)).contains("Invalid Email Format");
    }

    @ParameterizedTest(name = "password [{0}] rejected")
    @NullAndEmptySource
    @ValueSource(strings = {"12345", " "})
    void invalidPasswords(String password) {
        RegisterRequest r = valid();
        r.setPassword(password);
        assertThat(messagesOf(r))
                .anyMatch(m -> m.equals("Password is Required") || m.equals("Password must have atleast 6 characters"));
    }

    @ParameterizedTest(name = "firstName [{0}] requires NotBlank")
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void blankFirstName(String firstName) {
        RegisterRequest r = valid();
        r.setFirstName(firstName);
        assertThat(messagesOf(r)).contains("First name is required");
    }

    @ParameterizedTest(name = "lastName [{0}] requires NotBlank")
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void blankLastName(String lastName) {
        RegisterRequest r = valid();
        r.setLastName(lastName);
        assertThat(messagesOf(r)).contains("Last name is required");
    }

    private RegisterRequest valid() {
        RegisterRequest r = new RegisterRequest();
        r.setEmail("lokesh@example.com");
        r.setPassword("secret1");
        r.setKeycloakId("kc-123");
        r.setFirstName("Lokesh");
        r.setLastName("Siddi");
        return r;
    }

    private Set<String> messagesOf(RegisterRequest r) {
        return validator.validate(r).stream()
                .map(ConstraintViolation::getMessage)
                .collect(java.util.stream.Collectors.toSet());
    }
}
