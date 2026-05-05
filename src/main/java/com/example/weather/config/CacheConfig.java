package com.example.weather.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine-backed cache configuration for weather data.
 *
 * <p>Weather data is inherently short-lived; a time-to-live (TTL) of 10 minutes is a
 * reasonable balance between data freshness and reducing the number of outbound calls
 * to the OpenWeather API. Both the TTL and the maximum cache size are externalised to
 * {@code application.properties} so they can be tuned per environment without a rebuild:
 *
 * <pre>{@code
 * cache.weather.ttl-minutes=10
 * cache.weather.max-size=500
 * }</pre>
 *
 * <p>The single named cache registered here ({@code "weather"}) is referenced by the
 * {@code @Cacheable("weather")} annotation on
 * {@link com.example.weather.service.WeatherService#getWeather(String)}.
 *
 * <p>Cache statistics ({@code recordStats()}) are enabled so that hit/miss ratios
 * become visible through Spring Boot Actuator ({@code /actuator/metrics}) if the
 * Actuator dependency is added to the project in the future.
 */
@Configuration
public class CacheConfig {

    /**
     * How long a cached weather entry lives before it is evicted, in minutes.
     * Defaults to {@code 10} if the property is not set.
     */
    @Value("${cache.weather.ttl-minutes:10}")
    private long ttlMinutes;

    /**
     * Maximum number of city entries held in the cache at any one time.
     * When the limit is reached Caffeine evicts the least-recently-used entry.
     * Defaults to {@code 500} if the property is not set.
     */
    @Value("${cache.weather.max-size:500}")
    private long maxSize;

    /**
     * Creates the {@link CacheManager} bean backed by a Caffeine in-process cache.
     *
     * <p>The cache is configured with:
     * <ul>
     *   <li><b>expireAfterWrite</b> — entries expire a fixed duration after they are
     *       written, regardless of how often they are read. This guarantees data is
     *       never older than {@code ttlMinutes}.</li>
     *   <li><b>maximumSize</b> — bounds memory usage; entries are evicted using
     *       Caffeine's TinyLFU policy once the limit is reached.</li>
     *   <li><b>recordStats</b> — enables hit/miss/eviction counters, exposable via
     *       Spring Boot Actuator without any additional code changes.</li>
     * </ul>
     *
     * @return a {@link CaffeineCacheManager} managing the {@code "weather"} cache
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("weather");
        manager.setCaffeine(
                Caffeine.newBuilder()
                        .expireAfterWrite(ttlMinutes, TimeUnit.MINUTES)
                        .maximumSize(maxSize)
                        .recordStats()
        );
        return manager;
    }
}