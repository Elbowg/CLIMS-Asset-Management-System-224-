package com.clims.backend.exception;

/**
 * Thrown when a domain/business invariant is violated (e.g. assigning an already assigned asset
 * or invalid state transition).
 */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) { super(message); }
}
