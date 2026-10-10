package com.myplant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Plant DTO
 * Augmented with botanical, micro-location, anatomy, and IoT sensor telemetry.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlantDTO {
    private Long id;
    private String name;
    private String plantType;
    private String scientificName;
    private String plantFamily;
    private String potSize;
    private Boolean isIndoor;
    private String location;
    private Double latitude;
    private Double longitude;
    private LocalDate lastWateredDate;
    private Integer wateringStreak;
    private String health;
    private String notes;
    private String anatomyJson;
    private Integer customWateringInterval;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // IoT Sensor Status
    private boolean hasSensor;
    private SensorDeviceDTO sensorDevice;
    private SensorReadingDTO latestSensorReading;

    // Related care rule information
    private Long plantCareRuleId;
    private String careRuleDescription;
    private Integer baseWateringDays;
    private String wateringFrequency;
    private String sunlightNeeds;
    private String humidityPreference;
    private String temperatureRange;
    private String difficultyLevel;
    private String recommendedPotSize;
    private String growthRate;
    private String maxSize;
    private String careTips;
}
