package com.fitness.aiservice.exception.handler;

import com.fitness.aiservice.dto.ErrorResponse;
import com.fitness.aiservice.exception.GeminiApiException;
import com.fitness.aiservice.exception.RecommendationGenerationException;
import com.fitness.aiservice.exception.RecommendationNotFoundException;
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

    @ExceptionHandler(RecommendationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRecommendationNotFoundException(RecommendationNotFoundException ex,
                                                        HttpServletRequest request) {
        log.error("Recommendation not found: {}", ex.getMessage());
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(RecommendationGenerationException.class)
    public ResponseEntity<ErrorResponse> handleRecommendationGenerationException(RecommendationGenerationException ex,
                                                         HttpServletRequest request) {
        log.error("Recommendation generation failed: {}", ex.getMessage(), ex);
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_SERVER_ERROR",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    @ExceptionHandler(GeminiApiException.class)
    public ResponseEntity<ErrorResponse> handleGeminiApiException(GeminiApiException ex,
                                                                  HttpServletRequest request) {
        log.error("Gemini API error: {}", ex.getMessage(), ex);
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "AI Service Unavailable",
                "AI service is temporarily unavailable. Please try again later.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    // ===== GENERIC EXCEPTIONS =====

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                request.getRequestURI()
        );
        return ResponseEntity.internalServerError().body(body);
    }
}