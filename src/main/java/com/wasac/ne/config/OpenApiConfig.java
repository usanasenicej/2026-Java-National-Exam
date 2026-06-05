package com.wasac.ne.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI wasacOpenAPI() {
        final String securitySchemeName = "Bearer Authentication";
        return new OpenAPI()
                .info(new Info()
                        .title("WASAC Utility Billing System API")
                        .description("""
                                Backend API for Water and Sanitation Corporation (WASAC) Utility Billing System.
                                
                                **Roles:**
                                - ROLE_ADMIN: Configure tariffs, approve bills, manage users
                                - ROLE_OPERATOR: Capture meter readings
                                - ROLE_FINANCE: Approve bills and payments
                                - ROLE_CUSTOMER: View bills and payment history
                                
                                Authenticate via /api/auth/login, then use the JWT token in the Authorize button.
                                """)
                        .version("1.0.0")
                        .contact(new Contact().name("WASAC Dev Team").email("support@wasac.rw")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
