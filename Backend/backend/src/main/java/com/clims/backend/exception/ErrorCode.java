package com.clims.backend.exception;

/**
 * Canonical application error codes for consistent API responses.
 */
public enum ErrorCode {
    NOT_FOUND,
    VALIDATION_FAILED,
    BUSINESS_RULE_VIOLATION,
    DATA_INTEGRITY_VIOLATION,
    ACCESS_DENIED,
    AUTHENTICATION_FAILED,
    CONFLICT,
    INTERNAL_ERROR
}
