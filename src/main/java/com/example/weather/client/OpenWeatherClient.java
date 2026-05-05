package com.example.weather.client;

import com.example.weather.exception.CityNotFoundException;
import com.example.weather.exception.WeatherServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * HTTP adapter responsible for communicating with the OpenWeather API.
 *
 * <p>This class has a single responsibility: send HTTP requests to OpenWeather
 * and translate the responses (including errors) into domain objects or
 * domain exceptions. No business logic lives here.
 *
 * <p>Error mapping strategy:
 * <ul>
 *   <li>404 Not Found     → {@link CityNotFoundException}</li>
 *   <li>401 Unauthorized  → {@link WeatherServiceException} (invalid API key)</li>
 *   <li>Other 4xx/5xx     → {@link WeatherServiceException} (generic API error)</li>
 *   <li>Network failure   → {@link WeatherServiceException} (unreachable)</li>
 * </ul>
 *
 * <p>By catching raw Spring HTTP exceptions here and re-throwing typed domain
 * exceptions, the service layer stays completely decoupled from HTTP concerns.
 */
@Component
public class OpenWeatherClient {

    private static final Logger log = LoggerFactory.getLogger(OpenWeatherClient.class);

    /**
     * Path segment appended to the base URL to reach the current-weather endpoint.
     * Full URL example: https://api.openweathermap.org/data/2.5/weather
     */
    private static final String WEATHER_PATH = "/weather";

    /**
     * Spring's synchronous HTTP client. Configured in {@link com.example.weather.config.AppConfig}
     * with connect and read timeouts to avoid hanging threads.
     */
    private final RestTemplate restTemplate;

    /**
     * Base URL of the OpenWeather API, injected from {@code application.properties}.
     * Example value: {@code https://api.openweathermap.org/data/2.5}
     * Kept configurable so it can be overridden in tests or different environments.
     */
    private final String baseUrl;

    /**
     * Secret API key required by OpenWeather for every request.
     * Injected from {@code application.properties} — never hardcoded.
     */
    private final String apiKey;

    /**
     * Creates a new {@code OpenWeatherClient}.
     * Spring calls this constructor automatically and injects all parameters.
     *
     * @param restTemplate the HTTP client bean
     * @param baseUrl      base URL read from {@code openweather.api.url}
     * @param apiKey       API key read from {@code openweather.api.key}
     */
    public OpenWeatherClient(
            RestTemplate restTemplate,
            @Value("${openweather.api.url}") String baseUrl,
            @Value("${openweather.api.key}") String apiKey) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    /**
     * Fetches the current weather for the given city from the OpenWeather API.
     *
     * <p>Builds the full request URL, executes a GET request, and deserialises
     * the JSON response into an {@link OpenWeatherResponse}.
     *
     * <p>The city name is expected to be already validated and trimmed by the
     * controller layer before reaching this method.
     *
     * <p>Example request URL produced:
     * <pre>
     * https://api.openweathermap.org/data/2.5/weather?q=London&units=metric&appid=abc123
     * </pre>
     *
     * @param city the city name to look up (e.g. "London", "Vienna")
     * @return the raw weather response from OpenWeather
     * @throws CityNotFoundException   if OpenWeather returns 404 (city does not exist)
     * @throws WeatherServiceException if the API key is invalid, another API error occurs,
     *                                 or the API cannot be reached at all
     */
    public OpenWeatherResponse fetchWeather(String city) {

        java.net.URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl + WEATHER_PATH)
                .queryParam("q", city)
                .queryParam("units", "metric")
                .queryParam("appid", apiKey)
                .build()
                .encode()
                .toUri();

        log.info("Calling OpenWeather API for city='{}'", city);

        try {
            return restTemplate.getForObject(uri, OpenWeatherResponse.class);

        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new CityNotFoundException(city);
            }
            if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new WeatherServiceException("Invalid OpenWeather API key");
            }
            throw new WeatherServiceException("OpenWeather API error: " + ex.getMessage());

        } catch (RestClientException ex) {
            throw new WeatherServiceException("Could not reach OpenWeather API: " + ex.getMessage());
        }
    }
}