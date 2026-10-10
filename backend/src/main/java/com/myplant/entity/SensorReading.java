package com.myplant.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * SensorReading Entity - Records telemetry from an IoT Sensor Rod
 * Supports:
 * - Soil Moisture (%)
 * - Soil Temperature (°C)
 * - Soil Electrical Conductivity EC (µS/cm)
 * - Soil pH (0-14)
 * - Ambient Temperature (°C)
 * - Ambient Relative Humidity (%)
 * - Light Intensity (lux)
 */
@Entity
@Table(name = "sensor_readings", indexes = {
    @Index(name = "idx_reading_device", columnList = "sensor_device_id"),
    @Index(name = "idx_reading_plant", columnList = "plant_id"),
    @Index(name = "idx_reading_time", columnList = "recorded_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SensorReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_device_id", nullable = false)
    @JsonIgnoreProperties({"user", "plant"})
    private SensorDevice sensorDevice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id")
    @JsonIgnoreProperties({"user", "sensorDevice", "wateringHistory"})
    private Plant plant;

    // Soil parameters
    private Double soilMoisture;        // e.g. 42.5 %
    private Double soilTemperature;     // e.g. 21.4 °C
    private Double soilEc;              // e.g. 1.2 mS/cm or 1200 µS/cm
    private Double soilPh;              // e.g. 6.5 pH

    // Environmental parameters
    private Double ambientTemperature;  // e.g. 24.1 °C
    private Double ambientHumidity;     // e.g. 58.0 %
    private Double lightLux;            // e.g. 2400 lux

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt = LocalDateTime.now();

    @Transient
    public boolean isStale() {
        if (recordedAt == null) return true;
        return recordedAt.isBefore(LocalDateTime.now().minusHours(2));
    }
}
