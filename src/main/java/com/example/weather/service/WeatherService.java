package com.example.weather.service;

import com.example.weather.client.OpenWeatherClient;
import com.example.weather.client.OpenWeatherResponse;
import com.example.weather.model.WeatherResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Orchestrates fetching, validating, and transforming weather data.
 *
 * <p>This service sits between the controller and the HTTP client, and is
 * responsible for three things:
 * <ol>
 *   <li><b>Validation</b> — rejects blank or malformed city names before any
 *       network call is made.</li>
 *   <li><b>Delegation</b> — passes the normalised city name to
 *       {@link OpenWeatherClient} and receives the raw API response.</li>
 *   <li><b>Mapping</b> — transforms the raw {@link OpenWeatherResponse} into the
 *       public {@link WeatherResponse} contract, including unit conversion.</li>
 * </ol>
 *
 * <p><b>Caching:</b> results are stored in the {@code "weather"} Caffeine cache
 * (configured in {@link com.example.weather.config.CacheConfig}) with a default
 * TTL of 10 minutes. The cache key is the lower-cased city name, so {@code "London"}
 * and {@code "london"} resolve to the same entry and only one upstream call is made.
 */
@Service
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

    /**
     * Conversion factor from metres per second (m/s) to kilometres per hour (km/h).
     * {@code 1 m/s = 3.6 km/h}.
     */
    private static final double MS_TO_KMH = 3.6;

    private final OpenWeatherClient openWeatherClient;

    /**
     * Creates a new {@code WeatherService}.
     * Spring injects the {@link OpenWeatherClient} dependency automatically.
     *
     * @param openWeatherClient the HTTP adapter for the OpenWeather API
     */
    public WeatherService(OpenWeatherClient openWeatherClient) {
        this.openWeatherClient = openWeatherClient;
    }

    /**
     * Returns the current weather for the given city.
     *
     * <p>The city name is validated and normalised before the upstream API is called.
     * Results are cached in the {@code "weather"} cache keyed by the lower-cased city
     * name, so repeated calls for the same city within the TTL window return the cached
     * value without hitting the network.
     *
     * @param city the city name to look up (expected to be already trimmed by the controller)
     * @return the current weather mapped to the public response model
     * @throws IllegalArgumentException                              if {@code city} is blank or contains invalid characters
     * @throws com.example.weather.exception.CityNotFoundException   if OpenWeather does not recognise the city
     * @throws com.example.weather.exception.WeatherServiceException if the upstream API is unreachable or returns an error
     */
    @Cacheable(value = "weather", key = "#city.toLowerCase()")
    public WeatherResponse getWeather(String city) {
        String normalizedCity = validateAndNormalizeCity(city);

        log.info("Fetching weather for city='{}'", normalizedCity);
        OpenWeatherResponse raw = openWeatherClient.fetchWeather(normalizedCity);
        return map(raw);
    }

    /**
     * Maps a raw {@link OpenWeatherResponse} from OpenWeather to the public
     * {@link WeatherResponse} model.
     *
     * <p>Mapping rules:
     * <ul>
     *   <li><b>condition</b> — taken from the first element of the {@code weather[]}
     *       array; defaults to {@code "unknown"} if the list is null or empty.</li>
     *   <li><b>temperature</b> — taken directly from {@code main.temp} (already in °C
     *       because the client requests {@code units=metric}); defaults to {@code 0.0}
     *       if {@code main} is null.</li>
     *   <li><b>windSpeed</b> — converted from m/s to km/h by multiplying by
     *       {@value MS_TO_KMH} and rounded to one decimal place; defaults to {@code 0.0}
     *       if {@code wind} is null.</li>
     * </ul>
     *
     * @param raw the raw response received from the OpenWeather API
     * @return the mapped and unit-converted weather response
     */
    private WeatherResponse map(OpenWeatherResponse raw) {
        String condition = raw.getWeather() != null && !raw.getWeather().isEmpty()
                ? raw.getWeather().get(0).getDescription()
                : "unknown";

        double tempCelsius = raw.getMain() != null ? raw.getMain().getTemp() : 0.0;

        double windKmh = raw.getWind() != null
                ? Math.round(raw.getWind().getSpeed() * MS_TO_KMH * 10.0) / 10.0
                : 0.0;

        return new WeatherResponse(condition, tempCelsius, windKmh);
    }

    /**
     * Validates and normalises a city name before it is sent to the upstream API.
     *
     * <p>Validation rules:
     * <ul>
     *   <li>Must not be {@code null}, empty, or whitespace-only.</li>
     *   <li>Must contain only Unicode letters ({@code \p{L}}), spaces, apostrophes
     *       ({@code '}), periods ({@code .}), commas ({@code ,}), and hyphens ({@code -}).
     *       This covers names such as {@code "São Paulo"}, {@code "St. John's"}, and
     *       {@code "Châlons-en-Champagne"} while rejecting injection attempts or
     *       clearly invalid input.</li>
     * </ul>
     *
     * <p>Normalisation: leading and trailing whitespace is trimmed.
     *
     * @param city the raw city name from the request parameter
     * @return the trimmed, validated city name ready for the API call
     * @throws IllegalArgumentException if the city is blank or contains invalid characters
     */
    public String validateAndNormalizeCity(String city) {
        if (city == null || city.trim().isEmpty()) {
            throw new IllegalArgumentException("City must not be blank");
        }

        String normalized = city.trim();

        if (!normalized.matches("^[\\p{L}\\s'.,-]+$")) {
            throw new IllegalArgumentException("Invalid city format");
        }

        return normalized;
    }
}