package com.fitness.activityservice.exception.handler;

import com.fitness.activityservice.dto.ErrorResponse;
import com.fitness.activityservice.exception.ActivityNotFoundException;
import com.fitness.activityservice.exception.InvalidActivityDataException;
import com.fitness.activityservice.exception.UserValidationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ===== CUSTOM EXCEPTIONS =====

    @ExceptionHandler(ActivityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleActivityNotFoundException(ActivityNotFoundException ex,
                                                        HttpServletRequest request) {
        log.error("Activity not found: {}", ex.getMessage());
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(InvalidActivityDataException.class)
    public ResponseEntity<ErrorResponse> handleInvalidActivityDataException(InvalidActivityDataException ex,
                                                         HttpServletRequest request) {
        log.error("Invalid activity data: {}", ex.getMessage());
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "BAD_REQUEST",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(UserValidationException.class)
    public ResponseEntity<ErrorResponse> handleUserValidationException(UserValidationException ex,
                                                         HttpServletRequest request) {
        log.error("User validation failed: {}", ex.getMessage());
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "BAD_REQUEST",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(body);
    }

    // ===== VALIDATION EXCEPTIONS =====

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        log.error("Validation errors: {}", errors);

        ErrorResponse body = ErrorResponse.ofValidation(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                "One or more fields are invalid",
                request.getRequestURI(),
                errors
        );
        return ResponseEntity.badRequest().body(body);
    }

    // ===== GENERIC EXCEPTIONS =====

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unexpected exception: {}", ex.getMessage(), ex);
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                request.getRequestURI()
        );
        return ResponseEntity.internalServerError().body(body);
    }
}