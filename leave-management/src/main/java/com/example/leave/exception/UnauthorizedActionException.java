package com.example.leave.exception;

/** Authenticated user is not allowed to perform this action. Maps to HTTP 403. */
public class UnauthorizedActionException extends RuntimeException {
    public UnauthorizedActionException(String message) {
        super(message);
    }
}
