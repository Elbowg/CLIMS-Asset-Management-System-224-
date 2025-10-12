package com.clims.backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;
import java.util.Map;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "CLIMS Asset Management API",
                version = "v1",
                description = "API for authentication, asset lifecycle, maintenance and administration. Includes JWT-based auth, audit logging, and standardized error model.",
                contact = @Contact(name = "CLIMS Team"),
                license = @io.swagger.v3.oas.annotations.info.License(name = "Proprietary")
        ),
        security = {@SecurityRequirement(name = "bearer-jwt")}
)
@SecurityScheme(
        name = "bearer-jwt",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer",
        description = "Provide the access token. Refresh handled via /api/auth/refresh."
)
public class OpenApiConfig {

    @Bean
    public OpenAPI baseOpenAPI() {
        // Do not set .info() here to avoid import collision with annotation-based Info; annotations above provide Info metadata.
        return new OpenAPI().components(componentsWithErrors());
    }

    @Bean
    public OpenApiCustomizer globalErrorResponsesCustomizer() {
        return openApi -> Optional.ofNullable(openApi.getPaths())
                .ifPresent(paths -> paths.values().forEach(pathItem -> pathItem.readOperations()
                        .forEach(op -> {
                            ApiResponses responses = Optional.ofNullable(op.getResponses()).orElseGet(ApiResponses::new);
                            addIfAbsent(responses, "400", new ApiResponse().$ref("#/components/responses/BadRequestError"));
                            addIfAbsent(responses, "401", new ApiResponse().$ref("#/components/responses/UnauthorizedError"));
                            addIfAbsent(responses, "403", new ApiResponse().$ref("#/components/responses/ForbiddenError"));
                            addIfAbsent(responses, "404", new ApiResponse().$ref("#/components/responses/NotFoundError"));
                            addIfAbsent(responses, "409", new ApiResponse().$ref("#/components/responses/ConflictError"));
                            addIfAbsent(responses, "500", new ApiResponse().$ref("#/components/responses/InternalServerError"));
                            op.setResponses(responses);
                        })));
    }

    private void addIfAbsent(ApiResponses responses, String code, ApiResponse apiResponse) {
        if (responses.get(code) == null) {
            responses.addApiResponse(code, apiResponse);
        }
    }

    private Components componentsWithErrors() {
        // details.fields: { <fieldName>: <message> }
        Schema<?> fieldsMap = new ObjectSchema()
                .additionalProperties(new StringSchema().description("violation message"))
                .description("field -> violation message");

        ObjectSchema details = new ObjectSchema();
        details.addProperty("fields", fieldsMap);
        details.description("Optional structured error details");

        ObjectSchema errorResponse = new ObjectSchema();
        errorResponse.addProperty("timestamp", new StringSchema().format("date-time").description("ISO-8601 instant"));
        errorResponse.addProperty("path", new StringSchema().description("Request path"));
        errorResponse.addProperty("status", new IntegerSchema().description("HTTP status code"));
        errorResponse.addProperty("error", new StringSchema().description("HTTP reason phrase"));
        errorResponse.addProperty("code", new StringSchema().description("Application error code"));
        errorResponse.addProperty("message", new StringSchema().description("Human-readable message"));
        errorResponse.addProperty("details", details);
        errorResponse.description("Standardized API error response");
        errorResponse.addRequiredItem("timestamp");
        errorResponse.addRequiredItem("path");
        errorResponse.addRequiredItem("status");
        errorResponse.addRequiredItem("error");
        errorResponse.addRequiredItem("code");
        errorResponse.addRequiredItem("message");

        Components components = new Components()
                .addSchemas("ErrorResponse", errorResponse);

        components
                .addResponses("BadRequestError", new ApiResponse()
                        .description("Bad Request. See Docs/API_ERRORS.md for the standardized error contract.")
                        .content(jsonErrorContent(Map.of(
                                "timestamp", "2025-10-10T10:00:00Z",
                                "path", "/api/assets",
                                "status", 400,
                                "error", "Bad Request",
                                "code", "VALIDATION_FAILED",
                                "message", "Validation failed",
                                "details", Map.of(
                                        "fields", Map.of(
                                                "serialNumber", "must not be blank",
                                                "name", "size must be between 3 and 64"
                                        )
                                )
                        ))))
                .addResponses("UnauthorizedError", new ApiResponse()
                        .description("Unauthorized. See Docs/API_ERRORS.md for the standardized error contract.")
                        .content(jsonErrorContent(Map.of(
                                "timestamp", "2025-10-10T10:00:00Z",
                                "path", "/api/auth/me",
                                "status", 401,
                                "error", "Unauthorized",
                                "code", "AUTHENTICATION_FAILED",
                                "message", "Missing or invalid credentials"
                        ))))
                .addResponses("ForbiddenError", new ApiResponse()
                        .description("Forbidden. See Docs/API_ERRORS.md for the standardized error contract.")
                        .content(jsonErrorContent(Map.of(
                                "timestamp", "2025-10-10T10:00:00Z",
                                "path", "/api/admin/outbox/queue-depth",
                                "status", 403,
                                "error", "Forbidden",
                                "code", "ACCESS_DENIED",
                                "message", "Access is denied"
                        ))))
                .addResponses("NotFoundError", new ApiResponse()
                        .description("Not Found. See Docs/API_ERRORS.md for the standardized error contract.")
                        .content(jsonErrorContent(Map.of(
                                "timestamp", "2025-10-10T10:00:00Z",
                                "path", "/api/assets/123",
                                "status", 404,
                                "error", "Not Found",
                                "code", "NOT_FOUND",
                                "message", "Asset not found with id 123"
                        ))))
                .addResponses("ConflictError", new ApiResponse()
                        .description("Conflict. See Docs/API_ERRORS.md for the standardized error contract.")
                        .content(jsonErrorContent(Map.of(
                                "timestamp", "2025-10-10T10:00:00Z",
                                "path", "/api/assets/123/assign",
                                "status", 409,
                                "error", "Conflict",
                                "code", "BUSINESS_RULE_VIOLATION",
                                "message", "Asset cannot be assigned when status=RETIRED"
                        ))))
                .addResponses("InternalServerError", new ApiResponse()
                        .description("Internal Server Error. See Docs/API_ERRORS.md for the standardized error contract.")
                        .content(jsonErrorContent(Map.of(
                                "timestamp", "2025-10-10T10:00:00Z",
                                "path", "/api/assets",
                                "status", 500,
                                "error", "Internal Server Error",
                                "code", "INTERNAL_ERROR",
                                "message", "An unexpected error occurred"
                        ))));

        return components;
    }

    private Content jsonErrorContent(Map<String, Object> example) {
        Schema<?> ref = new Schema<>().$ref("#/components/schemas/ErrorResponse");
        MediaType mediaType = new MediaType().schema(ref);
        if (example != null && !example.isEmpty()) {
            mediaType.example(example);
        }
        return new Content().addMediaType("application/json", mediaType);
    }
}
