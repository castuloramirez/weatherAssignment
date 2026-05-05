package com.example.weather;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Entry point for the Weather API Spring Boot application.
 *
 * <p>{@code @SpringBootApplication} is a composite annotation that enables:
 * <ul>
 *   <li><b>{@code @Configuration}</b> — marks this class as a source of bean definitions.</li>
 *   <li><b>{@code @EnableAutoConfiguration}</b> — lets Spring Boot auto-configure beans
 *       based on the dependencies present on the classpath (e.g. Tomcat, Jackson).</li>
 *   <li><b>{@code @ComponentScan}</b> — scans this package and all sub-packages for
 *       Spring-managed components ({@code @Service}, {@code @RestController}, etc.).</li>
 * </ul>
 *
 * <p>{@code @EnableCaching} activates Spring's annotation-driven cache management,
 * enabling {@code @Cacheable} on service methods to be intercepted by the
 * Caffeine-backed {@link com.example.weather.config.CacheConfig#cacheManager()}.
 * Without this annotation, {@code @Cacheable} annotations are silently ignored.
 */
@SpringBootApplication
@EnableCaching
public class WeatherApplication {

    /**
     * Bootstraps the Spring application context and starts the embedded Tomcat server.
     *
     * @param args optional command-line arguments forwarded to Spring Boot
     *             (e.g. {@code --server.port=8081} to override the default port)
     */
    public static void main(String[] args) {
        SpringApplication.run(WeatherApplication.class, args);
    }
}