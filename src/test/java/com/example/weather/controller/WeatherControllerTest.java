package com.example.weather.controller;

import com.example.weather.exception.CityNotFoundException;
import com.example.weather.exception.WeatherServiceException;
import com.example.weather.model.WeatherResponse;
import com.example.weather.service.WeatherService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link WeatherController}.
 *
 * <p>These tests verify the controller's responsibilities in isolation:
 * <ul>
 *   <li>Delegating to {@link WeatherService} with the correctly trimmed city name.</li>
 *   <li>Wrapping the service result in a {@code 200 OK} {@link ResponseEntity}.</li>
 *   <li>Letting domain exceptions propagate unchanged to the global exception handler.</li>
 * </ul>
 *
 * <p>{@link WeatherService} is mocked via {@code @Mock} and injected into the
 * controller via {@code @InjectMocks}, so no Spring context is started and
 * tests run purely as plain JUnit 5 + Mockito tests.
 *
 * <p>Input validation ({@code @NotBlank}, {@code @Size}) is enforced by Spring's
 * {@code @Validated} AOP proxy, which is not active in this test setup. Validation
 * behaviour is therefore intentionally not covered here — it belongs in a
 * {@code @WebMvcTest} or integration test where the full filter chain is active.
 */
@ExtendWith(MockitoExtension.class)
class WeatherControllerTest {

    @Mock
    WeatherService weatherService;

    @InjectMocks
    WeatherController weatherController;

    // ── Happy path ────────────────────────────────────────────────────────────

    /**
     * Verifies the happy path: a valid city name produces a {@code 200 OK} response
     * whose body contains all three weather fields from the service.
     */
    @Test
    void givenValidCity_returnsWeatherResponse() {
        WeatherResponse expected = new WeatherResponse("clear sky", 22.3, 14.4);
        when(weatherService.getWeather("London")).thenReturn(expected);

        ResponseEntity<WeatherResponse> result = weatherController.getWeather("London");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().condition()).isEqualTo("clear sky");
        assertThat(result.getBody().temperature()).isEqualTo(22.3);
        assertThat(result.getBody().windSpeed()).isEqualTo(14.4);
    }

    /**
     * Verifies that the controller trims leading and trailing whitespace from the
     * city parameter before passing it to the service, so {@code "  Berlin  "}
     * is treated identically to {@code "Berlin"}.
     */
    @Test
    void givenCityWithSpaces_trimsAndReturnsWeather() {
        WeatherResponse expected = new WeatherResponse("cloudy", 10.0, 5.0);
        when(weatherService.getWeather("Berlin")).thenReturn(expected);

        // Controller calls city.trim() before passing to service
        ResponseEntity<WeatherResponse> result = weatherController.getWeather("  Berlin  ");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().condition()).isEqualTo("cloudy");
    }

    // ── Error propagation ─────────────────────────────────────────────────────

    /**
     * Verifies that a {@link CityNotFoundException} thrown by the service is
     * propagated unchanged so the global exception handler can map it to
     * a {@code 404 Not Found} response.
     */
    @Test
    void givenUnknownCity_throwsCityNotFoundException() {
        when(weatherService.getWeather("Atlantis"))
                .thenThrow(new CityNotFoundException("Atlantis"));

        assertThatThrownBy(() -> weatherController.getWeather("Atlantis"))
                .isInstanceOf(CityNotFoundException.class)
                .hasMessageContaining("Atlantis");
    }

    /**
     * Verifies that a {@link WeatherServiceException} thrown by the service is
     * propagated unchanged so the global exception handler can map it to
     * a {@code 503 Service Unavailable} response.
     */
    @Test
    void givenUpstreamFailure_throwsWeatherServiceException() {
        when(weatherService.getWeather("London"))
                .thenThrow(new WeatherServiceException("timeout"));

        assertThatThrownBy(() -> weatherController.getWeather("London"))
                .isInstanceOf(WeatherServiceException.class)
                .hasMessageContaining("timeout");
    }

    // ── City name formats ─────────────────────────────────────────────────────

    /**
     * Verifies that city names containing spaces, accented characters, apostrophes,
     * and hyphens are all passed through to the service without modification and
     * result in a {@code 200 OK} response.
     *
     * <p>This guards against accidental encoding or rejection of valid international
     * city names at the controller layer.
     */
    @ParameterizedTest
    @ValueSource(strings = {"New York", "São Paulo", "St. John's", "Châlons-en-Champagne"})
    void givenCityWithSpecialChars_returnsWeather(String city) {
        WeatherResponse expected = new WeatherResponse("sunny", 25.0, 7.2);
        when(weatherService.getWeather(city)).thenReturn(expected);

        ResponseEntity<WeatherResponse> result = weatherController.getWeather(city);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}