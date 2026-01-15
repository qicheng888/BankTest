package com.bank.transaction.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

/**
 * OpenAPI Configuration for Swagger Documentation
 * 
 * Access Swagger UI at: http://localhost:8080/swagger-ui.html
 * Access API docs at: http://localhost:8080/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

        @Value("${server.port:8080}")
        private String serverPort;

        @Bean
        public OpenAPI customOpenAPI() {
                return new OpenAPI()
                                .info(new Info()
                                                .title("Transaction Management API")
                                                .version("1.0.0")
                                                .description("Bank Transaction Management System - RESTful API Documentation\n\n"
                                                                +
                                                                "This API provides endpoints for managing bank transactions including:\n"
                                                                +
                                                                "- Create new transactions\n" +
                                                                "- Query transactions by ID\n" +
                                                                "- List all transactions with pagination\n" +
                                                                "- Update existing transactions\n" +
                                                                "- Delete transactions")
                                                .contact(new Contact()
                                                                .name("Bank Development Team")
                                                                .email("dev@bank.com"))
                                                .license(new License()
                                                                .name("Apache 2.0")
                                                                .url("http://www.apache.org/licenses/LICENSE-2.0")))
                                .servers(Collections.singletonList(
                                                new Server()
                                                                .url("http://localhost:" + serverPort)
                                                                .description("Local Development Server")));
        }
}
