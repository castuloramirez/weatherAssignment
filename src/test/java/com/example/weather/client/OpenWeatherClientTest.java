package com.example.weather.client;

import com.example.weather.exception.CityNotFoundException;
import com.example.weather.exception.WeatherServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OpenWeatherClient}.
 *
 * <p>These tests verify that the client correctly maps HTTP responses from
 * the OpenWeather API into domain objects or domain exceptions, without
 * making any real network calls.
 *
 * <p>{@link RestTemplate} is mocked so tests run offline and in isolation.
 * The client is constructed directly in {@link #setUp()} with a fake base URL
 * and API key, keeping the tests independent of {@code application.properties}.
 *
 * <p>Stubs use {@code any(URI.class)} rather than a specific URL matcher because
 * the exact URI is an implementation detail of the client; these tests focus on
 * behaviour (response mapping and error handling), not URL construction.
 */
@ExtendWith(MockitoExtension.class)
class OpenWeatherClientTest {

    @Mock
    RestTemplate restTemplate;

    OpenWeatherClient client;

    /**
     * Creates a fresh {@link OpenWeatherClient} before each test using a fake
     * base URL and API key so no real network calls are ever attempted.
     */
    @BeforeEach
    void setUp() {
        client = new OpenWeatherClient(restTemplate, "http://fake-api.com", "test-key");
    }

    /**
     * Verifies that a successful {@code 200 OK} response is deserialised correctly
     * and all three fields (condition description, temperature, wind speed) are
     * accessible on the returned object.
     */
    @Test
    void givenSuccessResponse_parsesFieldsCorrectly() {
        // Build the fake response directly in Java — no JSON needed
        OpenWeatherResponse.WeatherCondition condition = new OpenWeatherResponse.WeatherCondition("overcast clouds");
        OpenWeatherResponse fakeResponse = new OpenWeatherResponse(
                List.of(condition),
                new OpenWeatherResponse.Main(12.3),
                new OpenWeatherResponse.Wind(4.1)
        );

        when(restTemplate.getForObject(any(URI.class), eq(OpenWeatherResponse.class)))
                .thenReturn(fakeResponse);

        OpenWeatherResponse result = client.fetchWeather("London");

        assertThat(result.getWeather().get(0).getDescription()).isEqualTo("overcast clouds");
        assertThat(result.getMain().getTemp()).isEqualTo(12.3);
        assertThat(result.getWind().getSpeed()).isEqualTo(4.1);
    }

    /**
     * Verifies that a {@code 404 Not Found} response from the upstream API is
     * translated into a {@link CityNotFoundException} containing the requested
     * city name, rather than leaking a Spring HTTP exception to the caller.
     */
    @Test
    void given404Response_throwsCityNotFoundException() {
        when(restTemplate.getForObject(any(URI.class), eq(OpenWeatherResponse.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.fetchWeather("Nowhere"))
                .isInstanceOf(CityNotFoundException.class)
                .hasMessageContaining("Nowhere");
    }

    /**
     * Verifies that a {@code 401 Unauthorized} response — indicating an invalid
     * or missing API key — is translated into a {@link WeatherServiceException}
     * with a message referencing the API key.
     */
    @Test
    void given401Response_throwsWeatherServiceException() {
        when(restTemplate.getForObject(any(URI.class), eq(OpenWeatherResponse.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.fetchWeather("London"))
                .isInstanceOf(WeatherServiceException.class)
                .hasMessageContaining("API key");
    }

    /**
     * Verifies that a network-level failure (e.g. DNS error, connection refused,
     * timeout) is translated into a {@link WeatherServiceException} with a message
     * indicating the API could not be reached, rather than propagating a raw
     * {@link RestClientException} to the caller.
     */
    @Test
    void givenNetworkFailure_throwsWeatherServiceException() {
        when(restTemplate.getForObject(any(URI.class), eq(OpenWeatherResponse.class)))
                .thenThrow(new RestClientException("connection refused"));

        assertThatThrownBy(() -> client.fetchWeather("London"))
                .isInstanceOf(WeatherServiceException.class)
                .hasMessageContaining("Could not reach");
    }
}