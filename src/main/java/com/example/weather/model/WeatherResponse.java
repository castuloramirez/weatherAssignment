package com.example.weather.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Immutable response payload returned by {@code GET /api/v1/weather}.
 *
 * <p>This record is the public API contract between this service and its callers.
 * It is serialised to JSON by Jackson and sent as the HTTP response body.
 * {@code @JsonProperty} on each component ensures the JSON field names are
 * explicit and stable, independent of any future refactoring of the Java names.
 *
 * <p>Example JSON output:
 * <pre>{@code
 * {
 *   "condition":   "light rain",
 *   "temperature": 15.5,
 *   "wind_speed":  18.0
 * }
 * }</pre>
 *
 * <p>Units:
 * <ul>
 *   <li>{@code temperature} — degrees Celsius (°C)</li>
 *   <li>{@code wind_speed}  — kilometres per hour (km/h)</li>
 * </ul>
 *
 * <p>Values are derived from the raw {@link com.example.weather.client.OpenWeatherResponse}
 * by {@link com.example.weather.service.WeatherService}, which converts wind speed
 * from m/s to km/h and extracts the first weather condition description.
 */
public record WeatherResponse(

        /**
         * Human-readable weather condition description returned by OpenWeather,
         * e.g. {@code "clear sky"}, {@code "light rain"}, {@code "broken clouds"}.
         * Defaults to {@code "unknown"} when OpenWeather returns an empty condition list.
         */
        @JsonProperty("condition")
        String condition,

        /**
         * Current temperature in degrees Celsius (°C).
         */
        @JsonProperty("temperature")
        double temperature,

        /**
         * Current wind speed in kilometres per hour (km/h).
         * Converted from metres per second (m/s) by multiplying by {@code 3.6}.
         */
        @JsonProperty("wind_speed")
        double windSpeed

) {}