package com.example.leave.exception;

/** Requested days exceed the remaining balance. Maps to HTTP 400. */
public class InsufficientLeaveBalanceException extends InvalidLeaveRequestException {
    public InsufficientLeaveBalanceException(int requested, int available) {
        super("Insufficient leave balance: requested " + requested + " day(s) but only "
                + available + " day(s) available");
    }
}
