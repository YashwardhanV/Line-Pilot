package com.yashwardhanv.linepilot.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ApplicationConfig {

    // Timestamps are stored as UTC instants. The zone only decides which calendar day it is,
    // so token numbers restart at local midnight for the desk, not at midnight UTC.
    @Bean
    Clock clock(@Value("${app.time-zone}") String timeZone) {
        return Clock.system(ZoneId.of(timeZone));
    }

    @Bean
    OpenAPI linePilotOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("LinePilot API")
                        .version("v1")
                        .description("REST and SSE API for a transactional service queue"))
                .components(new Components().addSecuritySchemes("basicAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")));
    }
}
