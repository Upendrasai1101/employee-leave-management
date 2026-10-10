package com.example.leave.exception;

/** Bad input / rule violation (past dates, bad range, ...). Maps to HTTP 400. */
public class InvalidLeaveRequestException extends RuntimeException {
    public InvalidLeaveRequestException(String message) {
        super(message);
    }
}
