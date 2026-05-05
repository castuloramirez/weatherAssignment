package com.example.weather.service;

import com.example.weather.client.OpenWeatherClient;
import com.example.weather.client.OpenWeatherResponse;
import com.example.weather.exception.CityNotFoundException;
import com.example.weather.model.WeatherResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link WeatherService}.
 *
 * <p>These tests verify the service's two core responsibilities:
 * <ul>
 *   <li><b>Mapping</b> — raw {@link OpenWeatherResponse} fields are correctly
 *       transformed into the public {@link WeatherResponse} model, including
 *       unit conversion and defensive fallbacks.</li>
 *   <li><b>Exception propagation</b> — domain exceptions thrown by
 *       {@link OpenWeatherClient} pass through the service layer unchanged.</li>
 * </ul>
 *
 * <p>{@link OpenWeatherClient} is mocked so no HTTP calls are made.
 * {@code @InjectMocks} constructs {@link WeatherService} with the mock injected,
 * keeping the test completely independent of Spring context and configuration.
 *
 * <p>Caching ({@code @Cacheable}) is not active in this setup because no Spring
 * context is loaded. Cache behaviour is not the responsibility of this test class.
 */
@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    @Mock
    OpenWeatherClient openWeatherClient;

    @InjectMocks
    WeatherService weatherService;

    /**
     * A standard raw API response reused across mapping tests:
     * condition "light rain", temperature 15.5 °C, wind speed 5.0 m/s.
     */
    private OpenWeatherResponse rawResponse;

    /**
     * Initialises the shared {@link OpenWeatherResponse} fixture before each test.
     * Individual tests may override {@code rawResponse} if they need a different shape.
     */
    @BeforeEach
    void setUp() {
        rawResponse = new OpenWeatherResponse(
                List.of(new OpenWeatherResponse.WeatherCondition("light rain")),
                new OpenWeatherResponse.Main(15.5),
                new OpenWeatherResponse.Wind(5.0)
        );
    }

    /**
     * Verifies that the weather condition description from the first element of
     * the {@code weather[]} array is mapped to {@link WeatherResponse#condition()}.
     */
    @Test
    void mapsConditionCorrectly() {
        when(openWeatherClient.fetchWeather("Oslo")).thenReturn(rawResponse);
        assertThat(weatherService.getWeather("Oslo").condition()).isEqualTo("light rain");
    }

    /**
     * Verifies that the temperature value from {@code main.temp} is mapped
     * directly to {@link WeatherResponse#temperature()} without modification,
     * preserving the Celsius value returned by the API.
     */
    @Test
    void mapsTemperatureCorrectly() {
        when(openWeatherClient.fetchWeather("Oslo")).thenReturn(rawResponse);
        assertThat(weatherService.getWeather("Oslo").temperature()).isEqualTo(15.5);
    }

    /**
     * Verifies that wind speed is converted from metres per second (m/s) to
     * kilometres per hour (km/h) by multiplying by {@code 3.6} and rounding
     * to one decimal place.
     *
     * <p>Expected: {@code 5.0 m/s × 3.6 = 18.0 km/h}
     */
    @Test
    void convertsWindSpeedFromMsToKmh() {
        when(openWeatherClient.fetchWeather("Oslo")).thenReturn(rawResponse);
        // 5.0 m/s × 3.6 = 18.0 km/h
        assertThat(weatherService.getWeather("Oslo").windSpeed()).isEqualTo(18.0);
    }

    /**
     * Verifies the defensive fallback when the OpenWeather API returns an empty
     * {@code weather[]} array: the service should return {@code "unknown"} rather
     * than throwing an {@link IndexOutOfBoundsException}.
     */
    @Test
    void handlesEmptyWeatherList_returnsUnknownCondition() {
        rawResponse = new OpenWeatherResponse(
                List.of(),
                new OpenWeatherResponse.Main(15.5),
                new OpenWeatherResponse.Wind(5.0)
        );
        when(openWeatherClient.fetchWeather("Oslo")).thenReturn(rawResponse);
        assertThat(weatherService.getWeather("Oslo").condition()).isEqualTo("unknown");
    }

    /**
     * Verifies that a {@link CityNotFoundException} thrown by the client is not
     * swallowed or wrapped by the service layer, so it can reach the global
     * exception handler and be mapped to a {@code 404 Not Found} response.
     */
    @Test
    void propagatesCityNotFoundException() {
        when(openWeatherClient.fetchWeather("Atlantis")).thenThrow(new CityNotFoundException("Atlantis"));
        assertThatThrownBy(() -> weatherService.getWeather("Atlantis"))
                .isInstanceOf(CityNotFoundException.class)
                .hasMessageContaining("Atlantis");
    }
}