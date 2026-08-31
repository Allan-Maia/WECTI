package com.wecti.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI wectiOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("WECTI API")
                        .version("0.1.0-draft")
                        .description("API de controle de acesso a palestras e eventos"));
    }
}
