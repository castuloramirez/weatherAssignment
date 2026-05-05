package com.example.weather.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Central Spring configuration class for infrastructure beans.
 *
 * <p>Defines two beans:
 * <ul>
 *   <li>{@link RestTemplate} — the HTTP client used by {@link com.example.weather.client.OpenWeatherClient}
 *       to call the OpenWeather API.</li>
 *   <li>{@link OpenAPI} — the Springdoc metadata that drives the Swagger UI available
 *       at {@code /swagger-ui.html}.</li>
 * </ul>
 */
@Configuration
public class AppConfig {

    /**
     * Creates the {@link RestTemplate} bean used for all outbound HTTP calls.
     *
     * <p>{@link RestTemplateBuilder} is provided by Spring Boot and applies any
     * auto-configured defaults (e.g. message converters, error handlers).
     * Connect and read timeouts should be configured here if stricter latency
     * control is required, for example:
     * <pre>{@code
     * return builder
     *         .connectTimeout(Duration.ofSeconds(3))
     *         .readTimeout(Duration.ofSeconds(5))
     *         .build();
     * }</pre>
     *
     * @param builder Spring Boot's auto-configured builder
     * @return a fully initialised {@link RestTemplate}
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .build();
    }

    /**
     * Creates the {@link OpenAPI} bean that configures the Springdoc / Swagger UI.
     *
     * <p>The metadata defined here appears in the Swagger UI header at
     * {@code /swagger-ui.html} and in the raw OpenAPI spec at {@code /v3/api-docs}.
     *
     * @return the OpenAPI specification metadata for this application
     */
    @Bean
    public OpenAPI openApiSpec() {
        return new OpenAPI()
                .info(new Info()
                        .title("Weather API")
                        .version("1.0.0")
                        .description("Returns current weather for a city using the OpenWeather API")
                        .contact(new Contact().name("Weather API Team"))
                        .license(new License().name("MIT")));
    }
}