package com.clims.backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;

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
    public OpenAPI customOpenAPI() {
        // Define ErrorResponse schema programmatically so it's always present even if not directly referenced.
        Schema<?> errorSchema = new ObjectSchema()
                .addProperty("timestamp", new StringSchema().description("UTC instant").example(Instant.now().toString()))
                .addProperty("requestId", new StringSchema().description("Internal request ID (echoed as X-Request-Id)"))
                .addProperty("correlationId", new StringSchema().description("Cross-system correlation ID (X-Correlation-Id)"))
                .addProperty("path", new StringSchema())
                .addProperty("status", new IntegerSchema())
                .addProperty("error", new StringSchema())
                .addProperty("code", new StringSchema().description("Domain/business error code"))
                .addProperty("message", new StringSchema())
                .addProperty("details", new ObjectSchema().additionalProperties(true).description("Structured validation or contextual details"))
                .description("Standard error envelope");

        return new OpenAPI()
                .components(new Components()
                        .addSchemas("ErrorResponse", errorSchema));
    }
}
