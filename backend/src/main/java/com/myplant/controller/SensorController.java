package com.myplant.controller;

import com.myplant.dto.SensorDeviceDTO;
import com.myplant.dto.SensorReadingDTO;
import com.myplant.security.JwtTokenProvider;
import com.myplant.service.SensorService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * SensorController - Endpoints for IoT Sensor Rod (ESP32 / ESP8266) integration
 *
 * Endpoints:
 * - POST /api/sensors/register - Register a new sensor rod device
 * - GET /api/sensors/my-devices - Get list of user's registered sensor devices
 * - POST /api/sensors/{deviceId}/pair/{plantId} - Pair device to a plant
 * - POST /api/sensors/{deviceId}/unpair - Unpair device from a plant
 * - POST /api/sensors/{deviceId}/readings - Ingest telemetry from microcontroller
 * - GET /api/plants/{plantId}/sensors - Get plant sensor status (strictly empty if unpaired)
 */
@RestController
@RequestMapping("/api")
@AllArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174"})
public class SensorController {

    private final SensorService sensorService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Register a new sensor rod device
     */
    @PostMapping("/sensors/register")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SensorDeviceDTO> registerDevice(
            @RequestHeader("Authorization") String authorization,
            @RequestBody Map<String, String> payload) {

        String token = jwtTokenProvider.extractTokenFromBearerToken(authorization);
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        String deviceId = payload.get("deviceId");
        String name = payload.get("name");

        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new IllegalArgumentException("deviceId is required.");
        }

        SensorDeviceDTO device = sensorService.registerDevice(userId, deviceId.trim(), name);
        return ResponseEntity.ok(device);
    }

    /**
     * Get user's registered sensor devices
     */
    @GetMapping("/sensors/my-devices")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SensorDeviceDTO>> getMyDevices(
            @RequestHeader("Authorization") String authorization) {

        String token = jwtTokenProvider.extractTokenFromBearerToken(authorization);
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        List<SensorDeviceDTO> devices = sensorService.getUserDevices(userId);
        return ResponseEntity.ok(devices);
    }

    /**
     * Pair a sensor device to a plant
     */
    @PostMapping("/sensors/{deviceId}/pair/{plantId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SensorDeviceDTO> pairDevice(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String deviceId,
            @PathVariable Long plantId) {

        String token = jwtTokenProvider.extractTokenFromBearerToken(authorization);
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        SensorDeviceDTO paired = sensorService.pairDeviceToPlant(userId, deviceId, plantId);
        return ResponseEntity.ok(paired);
    }

    /**
     * Unpair a sensor device
     */
    @PostMapping("/sensors/{deviceId}/unpair")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SensorDeviceDTO> unpairDevice(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String deviceId) {

        String token = jwtTokenProvider.extractTokenFromBearerToken(authorization);
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        SensorDeviceDTO unpaired = sensorService.unpairDevice(userId, deviceId);
        return ResponseEntity.ok(unpaired);
    }

    /**
     * Ingest reading from IoT Sensor Rod (ESP32/ESP8266 HTTP POST)
     * Microcontroller sends:
     * {
     *   "pairingToken": "...",
     *   "soilMoisture": 42.5,
     *   "soilTemperature": 21.0,
     *   "soilEc": 1.15,
     *   "soilPh": 6.4,
     *   "ambientTemperature": 24.0,
     *   "ambientHumidity": 55.0,
     *   "lightLux": 2100.0,
     *   "batteryLevel": 95
     * }
     */
    @PostMapping("/sensors/{deviceId}/readings")
    public ResponseEntity<SensorReadingDTO> ingestReading(
            @PathVariable String deviceId,
            @RequestBody Map<String, Object> body) {

        Object tokenValue = body.get("pairingToken");
        String pairingToken = tokenValue == null ? null : tokenValue.toString();
        if (pairingToken == null || pairingToken.isBlank()) {
            throw new org.springframework.security.access.AccessDeniedException("pairingToken is required.");
        }

        Double soilMoisture = body.get("soilMoisture") != null ? Double.parseDouble(body.get("soilMoisture").toString()) : null;
        Double soilTemperature = body.get("soilTemperature") != null ? Double.parseDouble(body.get("soilTemperature").toString()) : null;
        Double soilEc = body.get("soilEc") != null ? Double.parseDouble(body.get("soilEc").toString()) : null;
        Double soilPh = body.get("soilPh") != null ? Double.parseDouble(body.get("soilPh").toString()) : null;
        Double ambientTemperature = body.get("ambientTemperature") != null ? Double.parseDouble(body.get("ambientTemperature").toString()) : null;
        Double ambientHumidity = body.get("ambientHumidity") != null ? Double.parseDouble(body.get("ambientHumidity").toString()) : null;
        Double lightLux = body.get("lightLux") != null ? Double.parseDouble(body.get("lightLux").toString()) : null;
        Integer batteryLevel = body.get("batteryLevel") != null ? Integer.parseInt(body.get("batteryLevel").toString()) : null;

        SensorReadingDTO recorded = sensorService.ingestReading(
                deviceId, pairingToken,
                soilMoisture, soilTemperature, soilEc, soilPh,
                ambientTemperature, ambientHumidity, lightLux, batteryLevel
        );

        return ResponseEntity.ok(recorded);
    }

    /**
     * Get sensor status and telemetry for a specific plant.
     * Enforces strict empty-state rule: if no device is paired, returns
     * empty measurements without inventing any fake values.
     */
    @GetMapping("/plants/{plantId}/sensors")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getPlantSensorStatus(
            @PathVariable Long plantId) {

        Map<String, Object> status = sensorService.getPlantSensorStatus(plantId);
        return ResponseEntity.ok(status);
    }
}
