package com.example.weather.controller;

import com.example.weather.model.WeatherResponse;
import com.example.weather.service.WeatherService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing the weather endpoint.
 *
 * <p>Base path: {@code /api/v1/weather}
 *
 * <p>{@code @Validated} activates Bean Validation on method parameters, meaning
 * constraint annotations ({@code @NotBlank}, {@code @Size}) on {@code @RequestParam}
 * arguments are enforced by Spring before the method body is entered. Violations
 * result in a {@link jakarta.validation.ConstraintViolationException} which should
 * be mapped to a {@code 400 Bad Request} response by a global exception handler.
 *
 * <p>This controller has no business logic — it is responsible only for:
 * <ol>
 *   <li>Validating and sanitising the incoming request parameter.</li>
 *   <li>Delegating to {@link WeatherService}.</li>
 *   <li>Wrapping the result in an appropriate {@link ResponseEntity}.</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/v1/weather")
@Validated
public class WeatherController {

    private final WeatherService weatherService;

    /**
     * Creates a new {@code WeatherController}.
     * Spring injects the {@link WeatherService} dependency automatically.
     *
     * @param weatherService the service that retrieves and caches weather data
     */
    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    /**
     * Returns the current weather for the given city.
     *
     * <p><b>Request:</b> {@code GET /api/v1/weather?city={city}}
     *
     * <p><b>Validation rules applied to {@code city}:</b>
     * <ul>
     *   <li>Must not be blank (null, empty, or whitespace-only).</li>
     *   <li>Must be between 2 and 100 characters after trimming.</li>
     * </ul>
     *
     * <p>Leading and trailing whitespace is trimmed before the value is passed to
     * the service, so {@code "  London  "} is treated the same as {@code "London"}.
     *
     * <p><b>Responses:</b>
     * <ul>
     *   <li>{@code 200 OK} — weather data found and returned successfully.</li>
     *   <li>{@code 400 Bad Request} — {@code city} parameter fails validation.</li>
     *   <li>{@code 404 Not Found} — city not recognised by the upstream API.</li>
     *   <li>{@code 502 Bad Gateway} — upstream OpenWeather API is unavailable.</li>
     * </ul>
     *
     * @param city the name of the city to look up (e.g. {@code "London"}, {@code "New York"})
     * @return {@code 200 OK} with a {@link WeatherResponse} body
     */
    @GetMapping
    public ResponseEntity<WeatherResponse> getWeather(
            @RequestParam("city")
            @NotBlank(message = "city must not be blank")
            @Size(min = 2, max = 100, message = "city must be between 2 and 100 characters")
            String city
    ) {
        WeatherResponse response = weatherService.getWeather(city.trim());
        return ResponseEntity.ok(response);
    }
}