package com.eventzone.backend.exception;

/** A valid request that conflicts with the current state (sold out, already cancelled, event closed). */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
