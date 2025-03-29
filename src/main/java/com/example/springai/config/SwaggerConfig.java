package com.example.springai.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI apiDocumentation() {
        return new OpenAPI()
                .addServersItem(new Server().url("/"))
                .info(new Info()
                        .title("AI Conversation Service API")
                        .version("2.0")
                        .description("Enhanced API for AI-powered conversations"));
    }
}
