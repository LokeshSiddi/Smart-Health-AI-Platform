package com.fitness.activityservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:AI Service}")
    private String appName;

    @Value("${server.port:8083}")
    private String serverPort;

    @Bean
    public OpenAPI aiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI Service API")
                        .description("API for AI-powered fitness recommendations using Google Gemini API in Smart Fit AI platform.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Lokesh Siddi")
                                .url("https://github.com/LokeshSiddi")
                                .email("lokeshsiddi06@gmail.com")))
                .components(new Components().addSecuritySchemes(
                        "bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local development server")
                ));
    }

}