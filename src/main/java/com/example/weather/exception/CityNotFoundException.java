package com.example.weather.exception;

/**
 * Thrown when the requested city cannot be found by the OpenWeather API.
 *
 * <p>This exception is raised by {@link com.example.weather.client.OpenWeatherClient}
 * when the upstream API returns a {@code 404 Not Found} response, and should be
 * mapped to a {@code 404 Not Found} HTTP response by the global exception handler.
 *
 * <p>Extends {@link RuntimeException} so callers are not forced to declare or
 * catch it — consistent with Spring's unchecked exception convention.
 */
public class CityNotFoundException extends RuntimeException {

    /**
     * Creates a new {@code CityNotFoundException} for the given city name.
     *
     * @param city the name of the city that could not be found (e.g. {@code "Atlantis"})
     */
    public CityNotFoundException(String city) {
        super("City not found: " + city);
    }
}