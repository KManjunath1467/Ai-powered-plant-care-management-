package com.myplant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SensorReadingDTO {
    private Long id;
    private String deviceId;
    private Long plantId;
    private Double soilMoisture;
    private Double soilTemperature;
    private Double soilEc;
    private Double soilPh;
    private Double ambientTemperature;
    private Double ambientHumidity;
    private Double lightLux;
    private LocalDateTime recordedAt;
    private boolean isStale;
}
