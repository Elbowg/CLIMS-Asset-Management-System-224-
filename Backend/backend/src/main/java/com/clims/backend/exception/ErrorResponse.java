package com.clims.backend.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.time.Instant;
import java.util.Map;

@JsonInclude(Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        String requestId,
        String correlationId,
        String path,
        int status,
        String error,
        String code,
        String message,
        Map<String, Object> details
) {
    public static ErrorResponse of(String path,
                                   int status,
                                   String error,
                                   ErrorCode code,
                                   String message,
                                   Map<String, Object> details,
                                   String requestId,
                                   String correlationId) {
        return new ErrorResponse(Instant.now(), requestId, correlationId, path, status, error, code.name(), message, details);
    }
}
