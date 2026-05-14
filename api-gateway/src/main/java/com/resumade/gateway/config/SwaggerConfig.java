package com.resumade.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }

    @Bean
    public GlobalOpenApiCustomizer rewriteServersCustomizer() {
        return openApi -> {
            // This ensures that all aggregated specs use the gateway as the base URL
            openApi.setServers(List.of(new Server().url("/").description("Gateway Server")));
            
            final String securitySchemeName = "bearerAuth";
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            openApi.getComponents().addSecuritySchemes(securitySchemeName,
                    new SecurityScheme()
                            .name(securitySchemeName)
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT"));

            // Add security requirement to all operations EXCEPT public ones
            if (openApi.getPaths() != null) {
                openApi.getPaths().forEach((path, pathItem) -> {
                    pathItem.readOperations().forEach(operation -> {
                        boolean isPublic = path.contains("/login") || 
                                           path.contains("/register") || 
                                           path.contains("/public") ||
                                           path.contains("/v3/api-docs");
                        
                        if (!isPublic) {
                            operation.addSecurityItem(new SecurityRequirement().addList(securitySchemeName));
                        }
                    });
                });
            }
        };
    }
}
