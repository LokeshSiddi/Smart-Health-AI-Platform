package com.fitness.activityservice.exception;

public class InvalidActivityDataException extends RuntimeException {
    public InvalidActivityDataException(String message) {
        super(message);
    }
}
