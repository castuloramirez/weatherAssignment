# Weather API

A Spring Boot REST API that fetches current weather from OpenWeather and returns it as clean JSON.

## Quick Start

```bash
# Set your OpenWeather API key (never commit the real key)
export OPENWEATHER_API_KEY=your-api-key-here   # Mac/Linux
set OPENWEATHER_API_KEY=your-api-key-here      # Windows

# Build & run
mvn spring-boot:run

# Test a city
curl "http://localhost:8080/api/v1/weather?city=London"

# Swagger UI
open http://localhost:8080/swagger-ui.html
```

## Example Response

```json
{
  "condition": "scattered clouds",
  "temperature": 18.5,
  "wind_speed": 14.4
}
```

## Design Decisions

### Architecture

```
WeatherController  →  WeatherService  →  OpenWeatherClient  →  OpenWeather API
  (validation,          (cache, mapping,     (HTTP adapter,
   HTTP status)          unit conversion)     error translation)
```

Three distinct layers with one responsibility each:
- **Controller** — HTTP concerns only: input validation, HTTP status codes, Swagger docs
- **Service** — Business logic: city name normalisation, caching, unit conversion (m/s → km/h), response mapping
- **Client** — HTTP adapter: URI construction, error translation into domain exceptions

### Input Validation

The `city` parameter goes through two validation stages:

1. **Bean Validation on the controller** (`@Validated`):
    - `@NotBlank` — rejects empty or whitespace-only values → `400`
    - `@Size(min=2, max=100)` — rejects single-char or absurdly long input → `400`

2. **Programmatic validation in `WeatherService.validateAndNormalizeCity()`**:
    - Regex `^[\p{L}\s'.,-]+$` — allows only Unicode letters, spaces, hyphens, apostrophes, commas, and dots
    - Covers real city names like *São Paulo*, *St. John's*, *Châlons-en-Champagne* while rejecting injection attempts

### Error Handling

All exceptions are mapped to plain-text HTTP responses by `GlobalExceptionHandler`:

| Situation | Exception | HTTP status |
|-----------|-----------|-------------|
| Invalid `city` param (blank, too short) | `ConstraintViolationException` | 400 |
| City not found (OpenWeather 404) | `CityNotFoundException` | 404 |
| OpenWeather unavailable / bad key | `WeatherServiceException` | 503 |

### Caching

Caffeine in-process cache (`"weather"` cache, keyed by lower-cased city name):
- **TTL**: 10 minutes — weather changes slowly; avoids hammering the free-tier rate limit
- **Max size**: 500 entries — bounded memory, evicts LRU automatically
- Cache key is normalised to lowercase so `London`, `london`, `LONDON` all share one entry
- Both values are configurable via `application.properties` without code changes:
  ```properties
  cache.weather.ttl-minutes=10
  cache.weather.max-size=500
  ```

### OpenAPI / Swagger

SpringDoc auto-generates the spec at runtime. Available at:
- Swagger UI: `GET /swagger-ui.html`
- JSON spec:  `GET /api-docs`

## Testing Strategy

Three test classes, each at the right layer:

### 1. `WeatherControllerTest` — plain Mockito, no Spring context
Mocks `WeatherService` via `@InjectMocks`. Verifies:
- `200 OK` with correct JSON fields on the happy path
- `city.trim()` is applied before the service is called
- `CityNotFoundException` and `WeatherServiceException` propagate unchanged to the global handler
- Parameterised test covers international city names (spaces, accents, apostrophes, hyphens)

### 2. `WeatherServiceTest` — plain Mockito, no Spring context
Mocks `OpenWeatherClient`. Verifies:
- Condition description mapped from `weather[0].description`
- Temperature passed through unchanged (already °C from API)
- Wind speed converted from m/s to km/h (`× 3.6`, rounded to 1 d.p.)
- Empty `weather[]` list → `"unknown"` condition (no `IndexOutOfBoundsException`)
- `CityNotFoundException` propagates without being wrapped

### 3. `OpenWeatherClientTest` — plain Mockito, no Spring context
Mocks `RestTemplate`. Verifies:
- Successful response is deserialised into `OpenWeatherResponse` correctly
- `404` → `CityNotFoundException` containing the city name
- `401` → `WeatherServiceException` mentioning the API key
- Network failure → `WeatherServiceException` mentioning "Could not reach"

### What would be added with more time
- `@WebMvcTest` controller tests with MockMvc — validates HTTP status codes, JSON shape, and Bean Validation rejection end-to-end
- WireMock integration tests for `OpenWeatherClient` — verifies real URI construction and full HTTP round-trip without mocking `RestTemplate`
- `@SpringBootTest` end-to-end test — full context including real Caffeine cache
- Cache behaviour tests — second call served from cache without hitting the client
- Rate limit handling and retry with exponential back-off (Spring Retry)

## Tech Stack

| Concern | Choice |
|---------|--------|
| Framework | Spring Boot 3.4.5 |
| HTTP client | `RestTemplate` (simple, synchronous; `WebClient` would be the choice for reactive) |
| Cache | Caffeine (in-process, low overhead) |
| API docs | SpringDoc / Swagger UI |
| Validation | Jakarta Bean Validation + programmatic regex |
| Error responses | Plain-text bodies via `GlobalExceptionHandler` |
| Java | 25 (records for immutable DTOs) |
