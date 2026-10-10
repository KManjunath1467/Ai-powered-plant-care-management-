package com.myplant.service;

import com.myplant.dto.PlantDTO;
import com.myplant.dto.SensorDeviceDTO;
import com.myplant.dto.SensorReadingDTO;
import com.myplant.entity.Plant;
import com.myplant.entity.PlantCareRule;
import com.myplant.entity.SensorDevice;
import com.myplant.entity.SensorReading;
import com.myplant.entity.User;
import com.myplant.exception.UnauthorizedException;
import com.myplant.repository.PlantCareRuleRepository;
import com.myplant.repository.PlantRepository;
import com.myplant.repository.SensorDeviceRepository;
import com.myplant.repository.SensorReadingRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Plant Service - Augmented with botanical taxonomy, micro-location,
 * anatomy information, and IoT sensor telemetry integration.
 */
@Service
@AllArgsConstructor
@Transactional
public class PlantService {

    private final PlantRepository plantRepository;
    private final PlantCareRuleRepository plantCareRuleRepository;
    private final SensorDeviceRepository sensorDeviceRepository;
    private final SensorReadingRepository sensorReadingRepository;
    private final UserService userService;

    /**
     * CREATE PLANT
     */
    public PlantDTO createPlant(PlantDTO dto, User user) {
        Plant plant = new Plant();
        plant.setName(dto.getName());
        plant.setPlantType(dto.getPlantType());
        plant.setScientificName(dto.getScientificName());
        plant.setPlantFamily(dto.getPlantFamily());
        plant.setPotSize(dto.getPotSize() != null ? dto.getPotSize() : "Medium");
        plant.setIsIndoor(dto.getIsIndoor() != null ? dto.getIsIndoor() : true);
        plant.setLocation(dto.getLocation() != null ? dto.getLocation() : "Home");
        plant.setLatitude(dto.getLatitude());
        plant.setLongitude(dto.getLongitude());
        plant.setNotes(dto.getNotes());
        plant.setAnatomyJson(dto.getAnatomyJson());
        plant.setImageUrl(dto.getImageUrl());
        plant.setCustomWateringInterval(dto.getCustomWateringInterval());
        plant.setLastWateredDate(LocalDate.now());
        plant.setCreatedAt(LocalDateTime.now());
        plant.setUpdatedAt(LocalDateTime.now());
        plant.setUser(user);

        // Find care rule using plant type
        PlantCareRule rule = plantCareRuleRepository
                .findByPlantNameIgnoreCase(dto.getPlantType())
                .orElse(null);
        plant.setPlantCareRule(rule);

        Plant savedPlant = plantRepository.save(plant);
        return convertToDTO(savedPlant);
    }

    /**
     * GET ALL USER PLANTS (ENTITY)
     */
    public List<Plant> getUserPlantEntities(Long userId) {
        return plantRepository.findByUser_Id(userId);
    }

    /**
     * GET ALL USER PLANTS (DTO)
     */
    public List<PlantDTO> getUserPlants(Long userId) {
        List<Plant> plants = plantRepository.findByUser_Id(userId);
        return plants.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * GET SINGLE PLANT
     */
    public PlantDTO getPlant(Long userId, Long plantId) {
        Plant plant = plantRepository
                .findByUserAndId(userService.getUserById(userId), plantId)
                .orElseThrow(() -> new UnauthorizedException(
                        "This plant does not belong to you or does not exist."
                ));
        return convertToDTO(plant);
    }

    /**
     * UPDATE PLANT
     */
    public PlantDTO updatePlant(Long userId, Long plantId, PlantDTO plantDTO) {
        User user = userService.getUserById(userId);
        Plant plant = plantRepository
                .findByUserAndId(user, plantId)
                .orElseThrow(() -> new UnauthorizedException(
                        "This plant does not belong to you or does not exist."
                ));

        plant.setName(plantDTO.getName());
        plant.setPlantType(plantDTO.getPlantType());
        if (plantDTO.getScientificName() != null) plant.setScientificName(plantDTO.getScientificName());
        if (plantDTO.getPlantFamily() != null) plant.setPlantFamily(plantDTO.getPlantFamily());
        if (plantDTO.getPotSize() != null) plant.setPotSize(plantDTO.getPotSize());
        if (plantDTO.getIsIndoor() != null) plant.setIsIndoor(plantDTO.getIsIndoor());
        if (plantDTO.getLocation() != null) plant.setLocation(plantDTO.getLocation());
        if (plantDTO.getLatitude() != null) plant.setLatitude(plantDTO.getLatitude());
        if (plantDTO.getLongitude() != null) plant.setLongitude(plantDTO.getLongitude());
        if (plantDTO.getNotes() != null) plant.setNotes(plantDTO.getNotes());
        if (plantDTO.getAnatomyJson() != null) plant.setAnatomyJson(plantDTO.getAnatomyJson());
        if (plantDTO.getImageUrl() != null) plant.setImageUrl(plantDTO.getImageUrl());
        if (plantDTO.getCustomWateringInterval() != null) {
            plant.setCustomWateringInterval(plantDTO.getCustomWateringInterval());
        }

        plant.setUpdatedAt(LocalDateTime.now());
        Plant updatedPlant = plantRepository.save(plant);
        return convertToDTO(updatedPlant);
    }

    /**
     * DELETE PLANT
     */
    public void deletePlant(Long userId, Long plantId) {
        User user = userService.getUserById(userId);
        Plant plant = plantRepository
                .findByUserAndId(user, plantId)
                .orElseThrow(() -> new UnauthorizedException(
                        "This plant does not belong to you or does not exist."
                ));

        // Unpair any associated sensor
        Optional<SensorDevice> deviceOpt = sensorDeviceRepository.findByPlantId(plantId);
        deviceOpt.ifPresent(d -> {
            d.setPlant(null);
            d.setStatus(SensorDevice.DeviceStatus.UNPAIRED);
            sensorDeviceRepository.save(d);
        });

        plantRepository.delete(plant);
    }

    /**
     * MARK AS WATERED
     */
    public PlantDTO markAsWatered(Long userId, Long plantId) {
        User user = userService.getUserById(userId);
        Plant plant = plantRepository
                .findByUserAndId(user, plantId)
                .orElseThrow(() -> new UnauthorizedException(
                        "This plant does not belong to you or does not exist."
                ));

        plant.setLastWateredDate(LocalDate.now());
        plant.setWateringStreak(plant.getWateringStreak() != null ? plant.getWateringStreak() + 1 : 1);
        plant.setUpdatedAt(LocalDateTime.now());
        Plant updatedPlant = plantRepository.save(plant);
        return convertToDTO(updatedPlant);
    }

    /**
     * UPDATE HEALTH
     */
    public PlantDTO updatePlantHealth(Long userId, Long plantId, String health) {
        User user = userService.getUserById(userId);
        Plant plant = plantRepository
                .findByUserAndId(user, plantId)
                .orElseThrow(() -> new UnauthorizedException(
                        "This plant does not belong to you or does not exist."
                ));

        plant.setHealth(health);
        plant.setUpdatedAt(LocalDateTime.now());
        Plant updatedPlant = plantRepository.save(plant);
        return convertToDTO(updatedPlant);
    }

    /**
     * ENTITY -> DTO
     */
    public PlantDTO convertToDTO(Plant plant) {
        PlantDTO dto = new PlantDTO();
        dto.setId(plant.getId());
        dto.setName(plant.getName());
        dto.setPlantType(plant.getPlantType());
        dto.setScientificName(plant.getScientificName());
        dto.setPlantFamily(plant.getPlantFamily());
        dto.setPotSize(plant.getPotSize());
        dto.setIsIndoor(plant.getIsIndoor());
        dto.setLocation(plant.getLocation());
        dto.setLatitude(plant.getLatitude());
        dto.setLongitude(plant.getLongitude());
        dto.setLastWateredDate(plant.getLastWateredDate());
        dto.setWateringStreak(plant.getWateringStreak());
        dto.setHealth(plant.getHealth());
        dto.setNotes(plant.getNotes());
        dto.setAnatomyJson(plant.getAnatomyJson());
        dto.setImageUrl(plant.getImageUrl());
        dto.setCustomWateringInterval(plant.getCustomWateringInterval());
        dto.setCreatedAt(plant.getCreatedAt());
        dto.setUpdatedAt(plant.getUpdatedAt());

        // IoT Sensor Telemetry Check
        Optional<SensorDevice> sensorOpt = sensorDeviceRepository.findByPlantId(plant.getId());
        if (sensorOpt.isPresent() && sensorOpt.get().getPlant() != null) {
            SensorDevice device = sensorOpt.get();
            dto.setHasSensor(true);

            SensorDeviceDTO deviceDTO = new SensorDeviceDTO();
            deviceDTO.setId(device.getId());
            deviceDTO.setDeviceId(device.getDeviceId());
            deviceDTO.setName(device.getName());
            deviceDTO.setStatus(device.getStatus().name());
            deviceDTO.setBatteryLevel(device.getBatteryLevel());
            deviceDTO.setFirmwareVersion(device.getFirmwareVersion());
            deviceDTO.setLastActiveAt(device.getLastActiveAt());
            deviceDTO.setStale(device.isStale());
            dto.setSensorDevice(deviceDTO);

            // Fetch latest reading
            Optional<SensorReading> latestReadingOpt = sensorReadingRepository.findLatestByPlantId(plant.getId());
            if (latestReadingOpt.isPresent()) {
                SensorReading r = latestReadingOpt.get();
                SensorReadingDTO rDTO = new SensorReadingDTO();
                rDTO.setId(r.getId());
                rDTO.setDeviceId(device.getDeviceId());
                rDTO.setPlantId(plant.getId());
                rDTO.setSoilMoisture(r.getSoilMoisture());
                rDTO.setSoilTemperature(r.getSoilTemperature());
                rDTO.setSoilEc(r.getSoilEc());
                rDTO.setSoilPh(r.getSoilPh());
                rDTO.setAmbientTemperature(r.getAmbientTemperature());
                rDTO.setAmbientHumidity(r.getAmbientHumidity());
                rDTO.setLightLux(r.getLightLux());
                rDTO.setRecordedAt(r.getRecordedAt());
                rDTO.setStale(r.isStale());
                dto.setLatestSensorReading(rDTO);
            }
        } else {
            // Strict empty state: explicitly false, null measurements
            dto.setHasSensor(false);
            dto.setSensorDevice(null);
            dto.setLatestSensorReading(null);
        }

        // Care Rule Information
        if (plant.getPlantCareRule() != null) {
            PlantCareRule rule = plant.getPlantCareRule();
            dto.setPlantCareRuleId(rule.getId());
            dto.setCareRuleDescription(rule.getDescription());
            dto.setBaseWateringDays(rule.getBaseWateringDays());
            dto.setWateringFrequency(rule.getWateringFrequency());
            dto.setSunlightNeeds(rule.getSunlightNeeds());
            dto.setHumidityPreference(rule.getHumidityPreference());
            dto.setTemperatureRange(rule.getTemperatureRange());
            dto.setDifficultyLevel(rule.getDifficultyLevel());
            dto.setRecommendedPotSize(rule.getRecommendedPotSize());
            dto.setGrowthRate(rule.getGrowthRate());
            dto.setMaxSize(rule.getMaxSize());
            dto.setCareTips(rule.getCareTips());
        }

        return dto;
    }
}