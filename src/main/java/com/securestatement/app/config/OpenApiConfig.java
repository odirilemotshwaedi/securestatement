package com.securestatement.app.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Secure Statement API",
                version = "1.0.0",
                description = "Generate account statements and retrieve secure statement download links."
        )
)
public class OpenApiConfig {
}

//http://localhost:8080/swagger-ui.html.
