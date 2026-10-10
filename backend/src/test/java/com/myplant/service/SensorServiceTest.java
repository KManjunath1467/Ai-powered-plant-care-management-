package com.myplant.service;

import com.myplant.dto.SensorDeviceDTO;
import com.myplant.entity.Plant;
import com.myplant.entity.SensorDevice;
import com.myplant.entity.SensorReading;
import com.myplant.entity.User;
import com.myplant.repository.PlantRepository;
import com.myplant.repository.SensorDeviceRepository;
import com.myplant.repository.SensorReadingRepository;
import com.myplant.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SensorServiceTest {

    @Mock
    private SensorDeviceRepository sensorDeviceRepository;
    @Mock
    private SensorReadingRepository sensorReadingRepository;
    @Mock
    private PlantRepository plantRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private GroqAIService groqAIService;

    private SensorService sensorService;

    @BeforeEach
    void setUp() {
        sensorService = new SensorService(
                sensorDeviceRepository,
                sensorReadingRepository,
                plantRepository,
                userRepository,
                groqAIService
        );
    }

    @Test
    @DisplayName("Mandatory Empty-State Rule: Plant without paired sensor rod must return empty measurements")
    void testMandatoryEmptyStateForUnpairedPlant() {
        Long plantId = 42L;
        Plant plant = new Plant();
        plant.setId(plantId);
        plant.setName("Unmonitored Fern");

        when(plantRepository.findById(plantId)).thenReturn(Optional.of(plant));
        when(sensorDeviceRepository.findByPlantId(plantId)).thenReturn(Optional.empty());

        Map<String, Object> status = sensorService.getPlantSensorStatus(plantId);

        assertNotNull(status);
        assertEquals(false, status.get("hasSensor"), "hasSensor must be strictly false");
        assertEquals("NO_SENSOR", status.get("status"));
        assertNull(status.get("latestReading"), "Measurements must be null for unpaired plant");
        assertEquals("Connect a compatible sensor rod to enable soil-based health monitoring for this plant.", status.get("message"));
    }

    @Test
    @DisplayName("Paired sensor rod returns actual telemetry readings and detects stale readings")
    void testPairedSensorWithLiveAndStaleReadings() {
        Long plantId = 10L;
        Plant plant = new Plant();
        plant.setId(plantId);
        plant.setName("Smart Monstera");

        SensorDevice device = new SensorDevice();
        device.setId(1L);
        device.setDeviceId("ROD-ESP32-99");
        device.setName("Monstera Rod");
        device.setPlant(plant);
        device.setStatus(SensorDevice.DeviceStatus.ONLINE);
        device.setLastActiveAt(LocalDateTime.now().minusHours(3)); // Stale > 2 hours

        SensorReading reading = new SensorReading();
        reading.setId(100L);
        reading.setSensorDevice(device);
        reading.setPlant(plant);
        reading.setSoilMoisture(45.0);
        reading.setSoilTemperature(22.0);
        reading.setRecordedAt(LocalDateTime.now().minusHours(3));

        when(plantRepository.findById(plantId)).thenReturn(Optional.of(plant));
        when(sensorDeviceRepository.findByPlantId(plantId)).thenReturn(Optional.of(device));
        when(sensorReadingRepository.findLatestByPlantId(plantId)).thenReturn(Optional.of(reading));

        Map<String, Object> status = sensorService.getPlantSensorStatus(plantId);

        assertNotNull(status);
        assertEquals(true, status.get("hasSensor"));
        assertEquals(true, status.get("isStale"), "Telemetry older than 2 hours must be marked stale");
        assertEquals("STALE", status.get("status"));
        assertNotNull(status.get("latestReading"));
    }
}
