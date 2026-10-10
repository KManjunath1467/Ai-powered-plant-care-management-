package com.myplant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myplant.dto.PlantDTO;
import com.myplant.dto.WeatherDTO;
import com.myplant.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Weather Service - Real Live Weather & Forecast Integration
 *
 * Uses Open-Meteo API (High precision, free, reliable, no API key required)
 * with OpenWeatherMap fallback and intelligent in-memory caching to respect rate limits.
 */
@Service
@Transactional(readOnly = true)
@Slf4j
public class WeatherService {

    @Value("${openweathermap.api.key:}")
    private String openWeatherApiKey;

    @Value("${openweathermap.api.url:https://api.openweathermap.org/data/2.5/weather}")
    private String openWeatherApiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final UserService userService;
    private final PlantService plantService;

    // Cache weather data for 15 minutes to prevent redundant network calls
    private final Map<String, CachedWeather> weatherCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 15 * 60 * 1000; // 15 minutes

    private static class CachedWeather {
        final WeatherDTO data;
        final long timestamp;
        CachedWeather(WeatherDTO data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }
        boolean isExpired() {
            return (System.currentTimeMillis() - timestamp) > CACHE_TTL_MS;
        }
    }

    public WeatherService(ObjectMapper objectMapper,
                          UserService userService,
                          PlantService plantService) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
        this.userService = userService;
        this.plantService = plantService;
    }

    /**
     * Fetch live weather for a city
     */
    public WeatherDTO getWeatherForCity(String city) {
        if (city == null || city.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A city or plant location is required for weather lookup.");
        }
        String cacheKey = "city:" + city.trim().toLowerCase();
        CachedWeather cached = weatherCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.data;
        }

        try {
            WeatherDTO weather = fetchFromOpenMeteo(city, null, null);
            if (weather != null) {
                weatherCache.put(cacheKey, new CachedWeather(weather));
                return weather;
            }
        } catch (Exception e) {
            log.warn("Open-Meteo fetch failed for {}: {}. Attempting fallback.", city, e.getMessage());
        }

        // Try OpenWeatherMap fallback if key is configured
        if (openWeatherApiKey != null && !openWeatherApiKey.isEmpty() && !openWeatherApiKey.contains("YOUR_OPENWEATHERMAP")) {
            try {
                WeatherDTO owmWeather = fetchFromOpenWeatherMap(city);
                if (owmWeather != null) {
                    weatherCache.put(cacheKey, new CachedWeather(owmWeather));
                    return owmWeather;
                }
            } catch (Exception ex) {
                log.warn("OpenWeatherMap fallback failed: {}", ex.getMessage());
            }
        }

        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Live weather is currently unavailable. No fabricated weather data was returned.");
    }

    /**
     * Fetch weather for specific coordinates (for precise plant location)
     */
    public WeatherDTO getWeatherForCoordinates(Double latitude, Double longitude, String label) {
        if (latitude == null || longitude == null || !Double.isFinite(latitude) || !Double.isFinite(longitude)
                || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid latitude and longitude are required.");
        }
        String cacheKey = String.format(Locale.US, "coord:%.2f,%.2f", latitude, longitude);
        CachedWeather cached = weatherCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.data;
        }

        try {
            WeatherDTO weather = fetchFromOpenMeteo(label != null ? label : "Custom Location", latitude, longitude);
            if (weather != null) {
                weatherCache.put(cacheKey, new CachedWeather(weather));
                return weather;
            }
        } catch (Exception e) {
            log.warn("Open-Meteo fetch failed for coords {},{}: {}", latitude, longitude, e.getMessage());
        }

        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Live weather is currently unavailable for these coordinates. No fabricated weather data was returned.");
    }

    /**
     * Open-Meteo Live Weather Fetcher
     */
    private WeatherDTO fetchFromOpenMeteo(String city, Double lat, Double lon) throws Exception {
        Double targetLat = lat;
        Double targetLon = lon;

        // If coordinates not provided, geocode city first
        if (targetLat == null || targetLon == null) {
            String geoUrl = String.format("https://geocoding-api.open-meteo.com/v1/search?name=%s&count=1&language=en&format=json",
                    city.replace(" ", "%20"));
            String geoResp = restTemplate.getForObject(geoUrl, String.class);
            JsonNode geoNode = objectMapper.readTree(geoResp);
            JsonNode results = geoNode.path("results");
            if (results.isArray() && results.size() > 0) {
                JsonNode first = results.get(0);
                targetLat = first.path("latitude").asDouble();
                targetLon = first.path("longitude").asDouble();
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No matching location was found for: " + city);
            }
        }

        String forecastUrl = String.format(Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,relative_humidity_2m,precipitation,rain,weather_code,wind_speed_10m,wind_direction_10m&hourly=temperature_2m,precipitation_probability,weather_code&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max&timezone=auto",
                targetLat, targetLon);

        String forecastResp = restTemplate.getForObject(forecastUrl, String.class);
        JsonNode root = objectMapper.readTree(forecastResp);

        WeatherDTO weather = new WeatherDTO();
        weather.setCity(city);
        weather.setLatitude(targetLat);
        weather.setLongitude(targetLon);
        weather.setLastUpdated(LocalDateTime.now());

        JsonNode current = root.path("current");
        if (current.isMissingNode()
                || !current.path("temperature_2m").isNumber()
                || !current.path("relative_humidity_2m").isNumber()
                || !current.path("precipitation").isNumber()
                || !current.path("weather_code").isNumber()
                || !current.path("wind_speed_10m").isNumber()
                || !current.path("wind_direction_10m").isNumber()) {
            throw new IllegalStateException("Weather provider response is missing required current-condition fields.");
        }
        double temp = current.path("temperature_2m").asDouble();
        double humidity = current.path("relative_humidity_2m").asDouble();
        double precipitation = current.path("precipitation").asDouble();
        int code = current.path("weather_code").asInt();
        double windSpeed = current.path("wind_speed_10m").asDouble();
        double windDir = current.path("wind_direction_10m").asDouble();

        weather.setTemperature(temp);
        weather.setHumidity(humidity);
        // Pressure is not requested from Open-Meteo here; leave it null instead of inventing 1013 hPa.
        weather.setWindSpeed(windSpeed);
        weather.setWindDirection(windDir);
        weather.setRainfall(precipitation);
        weather.setIsRaining(precipitation > 0.1 || isRainCode(code));
        weather.setWeatherMain(getWeatherConditionFromCode(code));
        weather.setWeatherDescription(getWeatherDescriptionFromCode(code));

        // Parse hourly forecast (next 24 hours)
        JsonNode hourly = root.path("hourly");
        if (!hourly.isMissingNode()) {
            JsonNode times = hourly.path("time");
            JsonNode temps = hourly.path("temperature_2m");
            JsonNode precips = hourly.path("precipitation_probability");
            JsonNode codes = hourly.path("weather_code");

            List<WeatherDTO.HourlyForecastDTO> hourlyList = new ArrayList<>();
            int count = Math.min(24, times.size());
            for (int i = 0; i < count; i++) {
                WeatherDTO.HourlyForecastDTO item = new WeatherDTO.HourlyForecastDTO();
                String rawTime = times.get(i).asText();
                item.setTime(rawTime.length() > 11 ? rawTime.substring(11, 16) : rawTime);
                item.setTemperature(temps.get(i).asDouble());
                item.setPrecipitationProbability(precips.get(i).asInt());
                item.setCondition(getWeatherConditionFromCode(codes.get(i).asInt()));
                hourlyList.add(item);
            }
            weather.setHourlyForecast(hourlyList);
            if (!hourlyList.isEmpty()) {
                weather.setPrecipitationProbability(hourlyList.get(0).getPrecipitationProbability());
            }
        }

        // Parse daily forecast (next 7 days)
        JsonNode daily = root.path("daily");
        if (!daily.isMissingNode()) {
            JsonNode dTimes = daily.path("time");
            JsonNode maxTemps = daily.path("temperature_2m_max");
            JsonNode minTemps = daily.path("temperature_2m_min");
            JsonNode dPrecips = daily.path("precipitation_probability_max");
            JsonNode dCodes = daily.path("weather_code");

            List<WeatherDTO.DailyForecastDTO> dailyList = new ArrayList<>();
            int count = Math.min(7, dTimes.size());
            for (int i = 0; i < count; i++) {
                WeatherDTO.DailyForecastDTO item = new WeatherDTO.DailyForecastDTO();
                item.setDate(dTimes.get(i).asText());
                item.setMaxTemp(maxTemps.get(i).asDouble());
                item.setMinTemp(minTemps.get(i).asDouble());
                item.setPrecipitationProbability(dPrecips.get(i).asInt());
                item.setCondition(getWeatherConditionFromCode(dCodes.get(i).asInt()));
                dailyList.add(item);
            }
            weather.setDailyForecast(dailyList);
        }

        return weather;
    }

    private WeatherDTO fetchFromOpenWeatherMap(String city) throws Exception {
        String url = String.format("%s?q=%s&appid=%s&units=metric", openWeatherApiUrl, city, openWeatherApiKey);
        String response = restTemplate.getForObject(url, String.class);
        JsonNode root = objectMapper.readTree(response);

        JsonNode main = root.path("main");
        if (!main.path("temp").isNumber() || !main.path("humidity").isNumber() || !main.path("pressure").isNumber()) {
            throw new IllegalStateException("OpenWeatherMap response is missing required current-condition fields.");
        }
        WeatherDTO weather = new WeatherDTO();
        weather.setCity(city);
        weather.setTemperature(main.path("temp").asDouble());
        weather.setHumidity(main.path("humidity").asDouble());
        weather.setPressure(main.path("pressure").asInt());

        JsonNode weatherArr = root.path("weather");
        if (weatherArr.isArray() && weatherArr.size() > 0) {
            weather.setWeatherMain(weatherArr.get(0).path("main").asText("Clear"));
            weather.setWeatherDescription(weatherArr.get(0).path("description").asText("clear sky"));
        }
        weather.setIsRaining("Rain".equalsIgnoreCase(weather.getWeatherMain()));
        weather.setLastUpdated(LocalDateTime.now());
        return weather;
    }

    private boolean isRainCode(int code) {
        return (code >= 51 && code <= 67) || (code >= 80 && code <= 82) || (code >= 95 && code <= 99);
    }

    private String getWeatherConditionFromCode(int code) {
        if (code == 0) return "Clear";
        if (code == 1 || code == 2 || code == 3) return "Partly Cloudy";
        if (code == 45 || code == 48) return "Foggy";
        if (code >= 51 && code <= 57) return "Drizzle";
        if (code >= 61 && code <= 67) return "Rain";
        if (code >= 71 && code <= 77) return "Snow";
        if (code >= 80 && code <= 82) return "Showers";
        if (code >= 95) return "Thunderstorm";
        return "Fair";
    }

    private String getWeatherDescriptionFromCode(int code) {
        if (code == 0) return "Clear sunny skies";
        if (code == 1) return "Mainly clear";
        if (code == 2) return "Partly cloudy";
        if (code == 3) return "Overcast conditions";
        if (code == 45 || code == 48) return "Foggy atmosphere";
        if (code >= 51 && code <= 55) return "Light drizzle";
        if (code >= 61 && code <= 65) return "Moderate rainfall";
        if (code >= 80 && code <= 82) return "Passing rain showers";
        if (code >= 95) return "Thunderstorms and heavy rain";
        return "Mild conditions";
    }

    public Map<String, Object> getCurrentWeather(String city) {
        WeatherDTO weather = getWeatherForCity(city);
        Map<String, Object> response = new HashMap<>();
        response.put("city", weather.getCity());
        response.put("temperature", weather.getTemperature());
        response.put("humidity", weather.getHumidity());
        response.put("weatherMain", weather.getWeatherMain());
        response.put("weatherDescription", weather.getWeatherDescription());
        response.put("isRaining", weather.getIsRaining());
        response.put("windSpeed", weather.getWindSpeed());
        response.put("rainfall", weather.getRainfall());
        response.put("precipitationProbability", weather.getPrecipitationProbability());
        response.put("lastUpdated", weather.getLastUpdated());
        response.put("hourlyForecast", weather.getHourlyForecast());
        response.put("dailyForecast", weather.getDailyForecast());
        response.put("careTip", buildWeatherCareTip(weather));
        return response;
    }

    public Map<String, Object> getWeatherRecommendations(Long userId, Long plantId) {
        User user = userService.getUserById(userId);
        PlantDTO plant = plantService.getPlant(userId, plantId);
        
        WeatherDTO weather;
        if (plant.getLatitude() != null && plant.getLongitude() != null) {
            weather = getWeatherForCoordinates(plant.getLatitude(), plant.getLongitude(), plant.getLocation());
        } else {
            weather = getWeatherForCity(user.getCity());
        }

        Map<String, Object> recommendation = new HashMap<>();
        recommendation.put("plant", plant);
        recommendation.put("weather", weather);
        recommendation.put("shouldSkipWatering", shouldSkipWatering(weather));
        recommendation.put("nextAction", buildWeatherWateringAction(plant, weather));
        recommendation.put("careTip", buildWeatherCareTip(weather));
        recommendation.put("recommendationTitle", Boolean.TRUE.equals(weather.getIsRaining()) ? "Rainy Day Care" : "Weather-Guided Care");
        return recommendation;
    }

    private String buildWeatherWateringAction(PlantDTO plant, WeatherDTO weather) {
        if (Boolean.TRUE.equals(weather.getIsRaining())) {
            return plant.getIsIndoor() 
                    ? "Indoor humidity is elevated due to rain outside. Soil dries slower today." 
                    : "Rain is falling. Skip supplemental watering and check container drainage.";
        }
        if (weather.getTemperature() != null && weather.getTemperature() > 30) {
            return "Warm weather accelerates evapotranspiration. Check soil dampness sooner than normal.";
        }
        if (weather.getHumidity() != null && weather.getHumidity() > 70) {
            return "High atmospheric humidity preserves soil moisture. Water only if soil feels dry 2 inches down.";
        }
        if (weather.getTemperature() != null && weather.getTemperature() < 12) {
            return "Cool conditions slow plant metabolism. Water sparingly to prevent root chill and rot.";
        }
        return "Ambient conditions are stable. Follow standard care rhythm.";
    }

    private String buildWeatherCareTip(WeatherDTO weather) {
        if (Boolean.TRUE.equals(weather.getIsRaining())) {
            return "Active rainfall lowers water loss. Monitor soil before your next hydration cycle.";
        }
        if (weather.getTemperature() != null && weather.getTemperature() > 30) {
            return "Hot conditions: shield sensitive plants from scorching mid-day rays.";
        }
        if (weather.getHumidity() != null && weather.getHumidity() > 70) {
            return "High ambient humidity: maintain airflow to prevent fungal moisture traps.";
        }
        if (weather.getTemperature() != null && weather.getTemperature() < 10) {
            return "Cold snap: keep tropical plants sheltered from drafty windows.";
        }
        return "Ideal ambient climate for healthy vegetative vitality.";
    }


    public boolean shouldSkipWatering(WeatherDTO weather) {
        if (weather.getIsRaining() != null && weather.getIsRaining()) {
            return true;
        }
        if (weather.getHumidity() != null && weather.getHumidity() > 85) {
            return true;
        }
        if (weather.getTemperature() != null && weather.getTemperature() < 5) {
            return true;
        }
        return false;
    }

    public Double getWateringIntervalMultiplier(WeatherDTO weather) {
        double multiplier = 1.0;
        if (weather.getTemperature() != null && weather.getTemperature() > 35) {
            multiplier *= 0.7;
        } else if (weather.getTemperature() != null && weather.getTemperature() > 28) {
            multiplier *= 0.85;
        } else if (weather.getTemperature() != null && weather.getTemperature() < 10) {
            multiplier *= 1.4;
        }

        if (weather.getHumidity() != null && weather.getHumidity() > 70) {
            multiplier *= 1.2;
        }
        return multiplier;
    }
}
