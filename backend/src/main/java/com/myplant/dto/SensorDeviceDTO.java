package com.myplant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SensorDeviceDTO {
    private Long id;
    private String deviceId;
    private String name;
    private Long plantId;
    private String plantName;
    private String status; // ONLINE, OFFLINE, UNPAIRED
    private Integer batteryLevel;
    private String firmwareVersion;
    private LocalDateTime lastActiveAt;
    private boolean isStale;
}
