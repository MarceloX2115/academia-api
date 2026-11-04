package com.academia.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI academiaOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("API de Gerenciamento de Academia")
                .version("1.0.0")
                .description("API REST para gerenciar alunos, planos e matrículas de uma academia, "
                        + "com controle de vencimento e regras de negócio."));
    }
}
