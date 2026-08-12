package com.fitness.userservice.exception.handler;

import com.fitness.userservice.dto.ErrorResponse;
import com.fitness.userservice.exception.InvalidUserDataException;
import com.fitness.userservice.exception.UserAlreadyExistsException;
import com.fitness.userservice.exception.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ===== CUSTOM EXCEPTIONS =====

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(UserNotFoundException ex,
                                                        HttpServletRequest request) {
        log.error("User not found: {}", ex.getMessage());
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExistsException(UserAlreadyExistsException ex,
                                                         HttpServletRequest request) {
        log.error("User already exists: {}", ex.getMessage());
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(InvalidUserDataException.class)
    public ResponseEntity<ErrorResponse> handleInvalidUserDataException(InvalidUserDataException ex,
                                                         HttpServletRequest request) {
        log.error("Invalid user data: {}", ex.getMessage());
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
                "BAD_REQUEST",
                ex.getMessage(),
                request.getRequestURI(),
                errors
        );
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex,
                                                               HttpServletRequest request) {
        log.error("Illegal argument: {}", ex.getMessage());
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(body);
    }

    // ===== GENERIC EXCEPTIONS =====

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex,
                                                                HttpServletRequest request) {
        log.error("Runtime exception: {}", ex.getMessage(), ex);
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An error occurred while processing your request",
                ex.getMessage(),
                request.getRequestURI()

        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex,
                                                                HttpServletRequest request) {
        log.error("Unexpected exception: {}", ex.getMessage(), ex);
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred. Please try again later.",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}