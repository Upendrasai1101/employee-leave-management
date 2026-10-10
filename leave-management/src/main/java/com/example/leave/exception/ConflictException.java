package com.example.leave.exception;

/** State conflict (duplicate email, overlapping leave, already reviewed). Maps to HTTP 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
