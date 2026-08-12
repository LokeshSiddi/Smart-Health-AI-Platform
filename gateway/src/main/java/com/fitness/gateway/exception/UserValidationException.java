package com.fitness.gateway.exception;

public class UserValidationException extends RuntimeException {
    public UserValidationException(String message) {
        super(message);
    }
}
