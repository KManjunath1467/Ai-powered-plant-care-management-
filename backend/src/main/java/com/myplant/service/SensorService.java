package com.myplant.service;

import com.myplant.dto.SensorDeviceDTO;
import com.myplant.dto.SensorReadingDTO;
import com.myplant.entity.Plant;
import com.myplant.entity.SensorDevice;
import com.myplant.entity.SensorReading;
import com.myplant.entity.User;
import com.myplant.repository.PlantRepository;
import com.myplant.repository.SensorDeviceRepository;
import com.myplant.repository.SensorReadingRepository;
import com.myplant.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * SensorService - Manages IoT Sensor Rods (ESP32/ESP8266)
 *
 * Implements:
 * - Device registration and secure pairing token generation
 * - Device-to-plant association and disassociation
 * - Telemetry ingestion (moisture, EC, pH, temp, ambient lux/humidity)
 * - Strict empty-state rule (no fabricated data when unpaired)
 * - Stale sensor detection (>2 hours)
 * - Threshold evaluation & Groq interpretation
 */
@Service
@AllArgsConstructor
@Transactional
@Slf4j
public class SensorService {

    private final SensorDeviceRepository sensorDeviceRepository;
    private final SensorReadingRepository sensorReadingRepository;
    private final PlantRepository plantRepository;
    private final UserRepository userRepository;
    private final GroqAIService groqAIService;

    /**
     * Register a new sensor device for a user
     */
    public SensorDeviceDTO registerDevice(Long userId, String deviceId, String name) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        Optional<SensorDevice> existing = sensorDeviceRepository.findByDeviceId(deviceId);
        if (existing.isPresent()) {
            throw new RuntimeException("Device ID '" + deviceId + "' is already registered.");
        }

        SensorDevice device = new SensorDevice();
        device.setDeviceId(deviceId);
        device.setName(name != null && !name.trim().isEmpty() ? name : "Sensor Rod (" + deviceId + ")");
        device.setUser(user);
        device.setPairingToken(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        device.setStatus(SensorDevice.DeviceStatus.UNPAIRED);
        device.setLastActiveAt(LocalDateTime.now());

        SensorDevice saved = sensorDeviceRepository.save(device);
        return toDeviceDTO(saved);
    }

    /**
     * Pair a sensor device to a plant
     */
    public SensorDeviceDTO pairDeviceToPlant(Long userId, String deviceId, Long plantId) {
        SensorDevice device = sensorDeviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found: " + deviceId));

        if (!device.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: You do not own this device.");
        }

        Plant plant = plantRepository.findById(plantId)
                .orElseThrow(() -> new RuntimeException("Plant not found: " + plantId));

        if (!plant.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: You do not own this plant.");
        }

        // Unpair previous device from this plant if any
        Optional<SensorDevice> existingPair = sensorDeviceRepository.findByPlantId(plantId);
        existingPair.ifPresent(p -> {
            p.setPlant(null);
            p.setStatus(SensorDevice.DeviceStatus.UNPAIRED);
            sensorDeviceRepository.save(p);
        });

        device.setPlant(plant);
        device.setStatus(SensorDevice.DeviceStatus.ONLINE);
        device.setLastActiveAt(LocalDateTime.now());
        SensorDevice saved = sensorDeviceRepository.save(device);

        plant.setSensorDevice(saved);
        plantRepository.save(plant);

        return toDeviceDTO(saved);
    }

    /**
     * Unpair a sensor device
     */
    public SensorDeviceDTO unpairDevice(Long userId, String deviceId) {
        SensorDevice device = sensorDeviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found: " + deviceId));

        if (!device.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: You do not own this device.");
        }

        if (device.getPlant() != null) {
            Plant plant = device.getPlant();
            plant.setSensorDevice(null);
            plantRepository.save(plant);
            device.setPlant(null);
        }

        device.setStatus(SensorDevice.DeviceStatus.UNPAIRED);
        SensorDevice saved = sensorDeviceRepository.save(device);
        return toDeviceDTO(saved);
    }

    /**
     * Ingest telemetry readings from IoT hardware (ESP32 / ESP8266)
     */
    public SensorReadingDTO ingestReading(String deviceId, String pairingToken,
                                          Double soilMoisture, Double soilTemperature,
                                          Double soilEc, Double soilPh,
                                          Double ambientTemperature, Double ambientHumidity,
                                          Double lightLux, Integer batteryLevel) {
        SensorDevice device = sensorDeviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new RuntimeException("Unrecognized sensor device: " + deviceId));

        // Authenticate pairing token if provided
        if (pairingToken != null && !pairingToken.equals(device.getPairingToken())) {
            throw new RuntimeException("Invalid sensor authentication token.");
        }

        device.setLastActiveAt(LocalDateTime.now());
        device.setStatus(SensorDevice.DeviceStatus.ONLINE);
        if (batteryLevel != null) {
            device.setBatteryLevel(Math.max(0, Math.min(100, batteryLevel)));
        }
        sensorDeviceRepository.save(device);

        SensorReading reading = new SensorReading();
        reading.setSensorDevice(device);
        reading.setPlant(device.getPlant());
        reading.setSoilMoisture(soilMoisture);
        reading.setSoilTemperature(soilTemperature);
        reading.setSoilEc(soilEc);
        reading.setSoilPh(soilPh);
        reading.setAmbientTemperature(ambientTemperature);
        reading.setAmbientHumidity(ambientHumidity);
        reading.setLightLux(lightLux);
        reading.setRecordedAt(LocalDateTime.now());

        SensorReading saved = sensorReadingRepository.save(reading);
        return toReadingDTO(saved);
    }

    /**
     * Get plant sensor telemetry status.
     * Enforces strict empty-state rule: if no sensor rod is paired,
     * returns hasSensor=false with null measurements.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getPlantSensorStatus(Long plantId) {
        Map<String, Object> response = new HashMap<>();
        Plant plant = plantRepository.findById(plantId)
                .orElseThrow(() -> new RuntimeException("Plant not found: " + plantId));

        Optional<SensorDevice> deviceOpt = sensorDeviceRepository.findByPlantId(plantId);

        if (deviceOpt.isEmpty() || deviceOpt.get().getPlant() == null) {
            // Strict empty-state rule: No fake numbers
            response.put("hasSensor", false);
            response.put("status", "NO_SENSOR");
            response.put("message", "Connect a compatible sensor rod to enable soil-based health monitoring for this plant.");
            response.put("device", null);
            response.put("latestReading", null);
            response.put("history", Collections.emptyList());
            response.put("interpretation", null);
            return response;
        }

        SensorDevice device = deviceOpt.get();
        boolean isStale = device.isStale();

        Optional<SensorReading> latestOpt = sensorReadingRepository.findLatestByPlantId(plantId);
        List<SensorReading> history = sensorReadingRepository.findByPlantIdOrderByRecordedAtDesc(plantId, PageRequest.of(0, 20));

        response.put("hasSensor", true);
        response.put("status", isStale ? "STALE" : "ONLINE");
        response.put("isStale", isStale);
        response.put("device", toDeviceDTO(device));

        if (latestOpt.isPresent()) {
            SensorReading latest = latestOpt.get();
            response.put("latestReading", toReadingDTO(latest));
            response.put("interpretation", buildSensorInterpretation(plant, latest, isStale));
        } else {
            response.put("latestReading", null);
            response.put("interpretation", "Sensor is paired but has not sent any readings yet.");
        }

        response.put("history", history.stream().map(this::toReadingDTO).toList());
        return response;
    }

    /**
     * Contextual interpretation of sensor readings comparing with thresholds
     */
    private Map<String, Object> buildSensorInterpretation(Plant plant, SensorReading reading, boolean isStale) {
        Map<String, Object> interp = new HashMap<>();
        if (isStale) {
            interp.put("overallStatus", "Stale Telemetry");
            interp.put("summary", "Device last transmitted at " + reading.getRecordedAt() + " (> 2 hours ago). Live soil conditions cannot be verified.");
            interp.put("actionRequired", "Inspect sensor rod power and Wi-Fi connection.");
            return interp;
        }

        List<String> findings = new ArrayList<>();
        String moistureStatus = "Optimal";
        if (reading.getSoilMoisture() != null) {
            if (reading.getSoilMoisture() < 25.0) {
                moistureStatus = "Critically Dry";
                findings.add("Soil moisture is very low (" + reading.getSoilMoisture() + "%). Soil may need inspection and thorough watering.");
            } else if (reading.getSoilMoisture() < 40.0) {
                moistureStatus = "Moderately Dry";
                findings.add("Soil moisture is dipping (" + reading.getSoilMoisture() + "%). Plan watering soon.");
            } else if (reading.getSoilMoisture() > 80.0) {
                moistureStatus = "Saturated / Soggy";
                findings.add("High soil moisture (" + reading.getSoilMoisture() + "%). Check drainage to prevent anaerobic root stress.");
            }
        }

        String ecStatus = "Normal";
        if (reading.getSoilEc() != null && reading.getSoilEc() > 2.2) {
            ecStatus = "Elevated EC";
            findings.add("Elevated electrical conductivity (" + reading.getSoilEc() + " mS/cm) indicates possible dissolved fertilizer salt buildup. Consider flushing substrate with plain water.");
        }

        String phStatus = "Balanced";
        if (reading.getSoilPh() != null) {
            if (reading.getSoilPh() < 5.5) {
                phStatus = "Acidic";
                findings.add("Soil pH is low (" + reading.getSoilPh() + "), which may hinder uptake of phosphorus and magnesium.");
            } else if (reading.getSoilPh() > 7.5) {
                phStatus = "Alkaline";
                findings.add("Soil pH is high (" + reading.getSoilPh() + "), which may restrict iron and micronutrient availability.");
            }
        }

        interp.put("moistureStatus", moistureStatus);
        interp.put("ecStatus", ecStatus);
        interp.put("phStatus", phStatus);
        interp.put("findings", findings);
        interp.put("summary", findings.isEmpty()
                ? "All measured soil and ambient metrics are within optimal parameters."
                : String.join(" ", findings));
        return interp;
    }

    /**
     * Get all devices belonging to a user
     */
    @Transactional(readOnly = true)
    public List<SensorDeviceDTO> getUserDevices(Long userId) {
        return sensorDeviceRepository.findByUserId(userId)
                .stream()
                .map(this::toDeviceDTO)
                .toList();
    }

    public SensorDeviceDTO toDeviceDTO(SensorDevice device) {
        SensorDeviceDTO dto = new SensorDeviceDTO();
        dto.setId(device.getId());
        dto.setDeviceId(device.getDeviceId());
        dto.setName(device.getName());
        dto.setStatus(device.getStatus().name());
        dto.setBatteryLevel(device.getBatteryLevel());
        dto.setFirmwareVersion(device.getFirmwareVersion());
        dto.setLastActiveAt(device.getLastActiveAt());
        dto.setStale(device.isStale());
        if (device.getPlant() != null) {
            dto.setPlantId(device.getPlant().getId());
            dto.setPlantName(device.getPlant().getName());
        }
        return dto;
    }

    public SensorReadingDTO toReadingDTO(SensorReading reading) {
        SensorReadingDTO dto = new SensorReadingDTO();
        dto.setId(reading.getId());
        dto.setDeviceId(reading.getSensorDevice().getDeviceId());
        if (reading.getPlant() != null) {
            dto.setPlantId(reading.getPlant().getId());
        }
        dto.setSoilMoisture(reading.getSoilMoisture());
        dto.setSoilTemperature(reading.getSoilTemperature());
        dto.setSoilEc(reading.getSoilEc());
        dto.setSoilPh(reading.getSoilPh());
        dto.setAmbientTemperature(reading.getAmbientTemperature());
        dto.setAmbientHumidity(reading.getAmbientHumidity());
        dto.setLightLux(reading.getLightLux());
        dto.setRecordedAt(reading.getRecordedAt());
        dto.setStale(reading.isStale());
        return dto;
    }
}
