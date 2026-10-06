package com.eventzone.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API documentation: Swagger UI at /swagger-ui.html, OpenAPI JSON at /v3/api-docs.
 * Use "Authorize" in Swagger UI and paste the token returned by POST /api/auth/login.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI eventZoneOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("EventZone API")
                        .version("v1")
                        .description("Event catalog, ticket booking, organiser and admin APIs. "
                                + "Errors use {timestamp, path, error, message}."))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
