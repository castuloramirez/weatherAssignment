package com.example.weather.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Centralised exception-to-HTTP-response mapping for the Weather API.
 *
 * <p>{@code @RestControllerAdvice} makes this class a global interceptor: any
 * exception thrown by a {@code @RestController} that is not caught locally will
 * be routed here before Spring writes the response to the client.
 *
 * <p>Error mapping summary:
 * <ul>
 *   <li>{@link ConstraintViolationException} → {@code 400 Bad Request} (invalid input)</li>
 *   <li>{@link CityNotFoundException}        → {@code 404 Not Found} (city unknown)</li>
 *   <li>{@link WeatherServiceException}      → {@code 503 Service Unavailable} (upstream failure)</li>
 * </ul>
 *
 * <p>All handlers return a plain-text body containing the exception message,
 * which is safe to expose to callers because every message is authored in this
 * codebase and contains no internal stack trace or sensitive detail.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles Bean Validation failures on {@code @RequestParam} and
     * {@code @PathVariable} arguments.
     *
     * <p>Raised by Spring when a constraint annotation such as {@code @NotBlank}
     * or {@code @Size} on a controller method parameter is violated.
     * The exception message contains each violated constraint and its message,
     * which is forwarded directly to the caller.
     *
     * @param ex the constraint violation exception, populated by Spring
     * @return {@code 400 Bad Request} with the violation details as plain text
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<String> handleValidation(ConstraintViolationException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ex.getMessage());
    }

    /**
     * Handles the case where the requested city is not recognised by OpenWeather.
     *
     * <p>Raised by {@link com.example.weather.client.OpenWeatherClient} when the
     * upstream API returns {@code 404 Not Found}. The exception message includes
     * the city name that was looked up.
     *
     * @param ex the city-not-found exception
     * @return {@code 404 Not Found} with the city name in the response body
     */
    @ExceptionHandler(CityNotFoundException.class)
    public ResponseEntity<String> handleCityNotFound(CityNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ex.getMessage());
    }

    /**
     * Handles failures originating from the OpenWeather API integration.
     *
     * <p>Raised by {@link com.example.weather.client.OpenWeatherClient} for any
     * upstream problem: invalid API key ({@code 401}), unexpected API error
     * (other {@code 4xx}), network timeout, or DNS failure. A {@code 503} response
     * signals to the caller that the problem is not with their request but with an
     * upstream dependency.
     *
     * @param ex the weather-service exception
     * @return {@code 503 Service Unavailable} with the error detail as plain text
     */
    @ExceptionHandler(WeatherServiceException.class)
    public ResponseEntity<String> handleWeatherService(WeatherServiceException ex) {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ex.getMessage());
    }
}