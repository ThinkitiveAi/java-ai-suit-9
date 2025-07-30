package com.example.session_demo.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.OAuthFlow;
import io.swagger.v3.oas.annotations.security.OAuthFlows;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@SecurityScheme(
    name = "OAuth2Auth", 
    type = SecuritySchemeType.OAUTH2, 
    in = SecuritySchemeIn.HEADER, 
    scheme = "bearer", 
    flows = @OAuthFlows(
        password = @OAuthFlow(tokenUrl = "${springdoc.swagger-ui.oauth.token-url}")
    )
)
@OpenAPIDefinition(
    info = @io.swagger.v3.oas.annotations.info.Info(
        title = "Healthcare Provider Registration API", 
        version = "1.0.0"
    ), 
    security = {@SecurityRequirement(name = "OAuth2Auth")}, 
    servers = {@io.swagger.v3.oas.annotations.servers.Server(url = "/", description = "Default Server URL")}
)
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI healthcareManagementOpenAPI() {
        Server devServer = new Server();
        devServer.setUrl("http://localhost:" + serverPort);
        devServer.setDescription("Development server");

        Contact contact = new Contact();
        contact.setEmail("admin@healthcare.com");
        contact.setName("Healthcare Management Team");
        contact.setUrl("https://healthcare.com");

        License license = new License().name("MIT License").url("https://choosealicense.com/licenses/mit/");

        Info info = new Info()
                .title("Healthcare Provider Registration API")
                .version("1.0.0")
                .contact(contact)
                .description("Secure REST API for healthcare provider registration with email verification, " +
                           "rate limiting, comprehensive validation, and enterprise-level security features.")
                .termsOfService("https://healthcare.com/terms")
                .license(license);

        return new OpenAPI()
                .info(info)
                .servers(List.of(devServer));
    }
} 