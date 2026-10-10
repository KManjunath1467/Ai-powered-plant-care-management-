package com.myplant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Weather DTO - Represents comprehensive live weather and forecast data
 * Sourced from Open-Meteo with caching and OpenWeatherMap fallback.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherDTO {
    private String city;
    private Double latitude;
    private Double longitude;
    private Double temperature;
    private Double humidity;
    private Integer pressure;
    private String weatherMain; // e.g. "Clear", "Rain", "Cloudy"
    private String weatherDescription; // e.g. "Sunny", "Scattered Showers"
    private Boolean isRaining;
    private Double windSpeed; // km/h
    private Double windDirection; // degrees
    private Double rainfall; // mm
    private Integer precipitationProbability; // %
    private LocalDateTime lastUpdated = LocalDateTime.now();

    // Hourly forecast for next 24 hours
    private List<HourlyForecastDTO> hourlyForecast;

    // Daily forecast for next 7 days
    private List<DailyForecastDTO> dailyForecast;

    // Alias for getDescription
    public String getDescription() {
        return weatherDescription != null ? weatherDescription : weatherMain;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HourlyForecastDTO {
        private String time;
        private Double temperature;
        private Integer precipitationProbability;
        private String condition;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyForecastDTO {
        private String date;
        private Double maxTemp;
        private Double minTemp;
        private Integer precipitationProbability;
        private String condition;
    }
}
