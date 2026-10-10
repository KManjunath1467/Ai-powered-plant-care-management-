package com.myplant.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * SensorDevice Entity - Represents an IoT Sensor Rod (ESP32/ESP8266)
 * Supports secure device pairing, device-to-plant association, and connectivity monitoring.
 */
@Entity
@Table(name = "sensor_devices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SensorDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", unique = true, nullable = false)
    private String deviceId; // e.g. "ROD-ESP32-84F3"

    @Column(nullable = false)
    private String name; // e.g. "Living Room Sensor Rod #1"

    @JsonIgnore
    @Column(name = "pairing_token", nullable = false)
    private String pairingToken; // Device auth token

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"password", "plants", "notifications", "sensorDevices"})
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id")
    @JsonIgnoreProperties({"user", "sensorDevice", "wateringHistory", "careRecommendations"})
    private Plant plant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceStatus status = DeviceStatus.UNPAIRED;

    private Integer batteryLevel = 100;

    private String firmwareVersion = "v1.2.0-esp32";

    private LocalDateTime lastActiveAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum DeviceStatus {
        ONLINE,
        OFFLINE,
        UNPAIRED
    }

    /**
     * Helper to verify if the reading is stale (> 2 hours old)
     */
    public boolean isStale() {
        if (lastActiveAt == null) return true;
        return lastActiveAt.isBefore(LocalDateTime.now().minusHours(2));
    }
}
