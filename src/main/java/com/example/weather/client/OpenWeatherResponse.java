package com.example.weather.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents the subset of fields returned by the OpenWeather current-weather endpoint
 * ({@code GET /data/2.5/weather}) that this application cares about.
 *
 * <p>OpenWeather returns a large JSON object; {@code @JsonIgnoreProperties(ignoreUnknown = true)}
 * on every class ensures Jackson silently discards any fields not mapped here, so the
 * application stays resilient to API additions or changes.
 *
 * <p>Example JSON structure (abbreviated):
 * <pre>{@code
 * {
 *   "weather": [{ "description": "light rain" }],
 *   "main":    { "temp": 15.5 },
 *   "wind":    { "speed": 3.2 }
 * }
 * }</pre>
 *
 * <p>The no-arg constructors on this class and all inner classes are required by Jackson
 * for deserialisation. The parameterised constructors exist purely for convenient
 * test fixture creation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenWeatherResponse {

    /**
     * List of weather condition objects. OpenWeather always returns at least one element,
     * but the service layer treats an empty list defensively by returning "unknown".
     */
    @JsonProperty("weather")
    private List<WeatherCondition> weather;

    /** Temperature and related atmospheric measurements. */
    @JsonProperty("main")
    private Main main;

    /** Wind measurements. */
    @JsonProperty("wind")
    private Wind wind;

    /** No-arg constructor required by Jackson for deserialisation. */
    public OpenWeatherResponse() {}

    /**
     * Convenience constructor for tests — allows building a complete response
     * without going through Jackson.
     *
     * @param weather list of weather conditions
     * @param main    temperature data
     * @param wind    wind data
     */
    public OpenWeatherResponse(List<WeatherCondition> weather, Main main, Wind wind) {
        this.weather = weather;
        this.main = main;
        this.wind = wind;
    }

    /** @return list of weather condition descriptors (e.g. "light rain") */
    public List<WeatherCondition> getWeather() { return weather; }

    /** @return main atmospheric data including temperature */
    public Main getMain() { return main; }

    /** @return wind data including speed */
    public Wind getWind() { return wind; }

    // ── Inner classes ─────────────────────────────────────────────────────────

    /**
     * Maps the {@code weather[]} array element from the OpenWeather response.
     * Each element can carry an {@code id}, {@code main}, {@code description},
     * and {@code icon}; only {@code description} is mapped here.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WeatherCondition {

        /**
         * Human-readable weather description, e.g. {@code "light rain"},
         * {@code "clear sky"}, {@code "broken clouds"}.
         */
        @JsonProperty("description")
        private String description;

        /** No-arg constructor required by Jackson for deserialisation. */
        public WeatherCondition() {}

        /**
         * Convenience constructor for tests.
         *
         * @param description weather description string
         */
        public WeatherCondition(String description) {
            this.description = description;
        }

        /** @return weather description (e.g. "scattered clouds") */
        public String getDescription() { return description; }
    }

    /**
     * Maps the {@code main} object from the OpenWeather response.
     * The full object also contains {@code feels_like}, {@code humidity},
     * {@code pressure}, etc.; only {@code temp} is mapped here.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Main {

        /**
         * Current temperature in Celsius (metric units are requested by
         * {@link OpenWeatherClient} via the {@code units=metric} query parameter).
         */
        @JsonProperty("temp")
        private double temp;

        /** No-arg constructor required by Jackson for deserialisation. */
        public Main() {}

        /**
         * Convenience constructor for tests.
         *
         * @param temp temperature in Celsius
         */
        public Main(double temp) {
            this.temp = temp;
        }

        /** @return temperature in Celsius */
        public double getTemp() { return temp; }
    }

    /**
     * Maps the {@code wind} object from the OpenWeather response.
     * The full object also contains {@code deg} (direction) and {@code gust};
     * only {@code speed} is mapped here.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Wind {

        /**
         * Wind speed in metres per second (m/s). The service layer converts
         * this to km/h before returning it to callers.
         */
        @JsonProperty("speed")
        private double speed;

        /** No-arg constructor required by Jackson for deserialisation. */
        public Wind() {}

        /**
         * Convenience constructor for tests.
         *
         * @param speed wind speed in m/s
         */
        public Wind(double speed) {
            this.speed = speed;
        }

        /** @return wind speed in m/s */
        public double getSpeed() { return speed; }
    }
}