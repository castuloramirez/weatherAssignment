package com.example.weather.exception;

/**
 * Thrown when the OpenWeather API cannot be reached or returns an unexpected error.
 *
 * <p>This exception is raised by {@link com.example.weather.client.OpenWeatherClient}
 * in three situations:
 * <ul>
 *   <li><b>Invalid API key</b> — the upstream API returns {@code 401 Unauthorized}.</li>
 *   <li><b>Unexpected API error</b> — any other {@code 4xx} response not handled
 *       by a more specific exception.</li>
 *   <li><b>Network failure</b> — DNS errors, connection timeouts, or any other
 *       {@link org.springframework.web.client.RestClientException}.</li>
 * </ul>
 *
 * <p>It is mapped to a {@code 503 Service Unavailable} HTTP response by
 * {@link GlobalExceptionHandler}, signalling to the caller that the problem
 * lies with an upstream dependency rather than their request.
 *
 * <p>Extends {@link RuntimeException} so callers are not forced to declare or
 * catch it — consistent with Spring's unchecked exception convention.
 */
public class WeatherServiceException extends RuntimeException {

    /**
     * Creates a new {@code WeatherServiceException} with the given detail message.
     *
     * @param message a description of the failure (e.g. {@code "Invalid OpenWeather API key"},
     *                {@code "Could not reach OpenWeather API: connection refused"})
     */
    public WeatherServiceException(String message) {
        super(message);
    }
}