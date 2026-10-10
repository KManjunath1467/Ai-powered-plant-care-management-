package com.myplant.repository;

import com.myplant.entity.Plant;
import com.myplant.entity.SensorDevice;
import com.myplant.entity.SensorReading;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {
    List<SensorReading> findBySensorDeviceOrderByRecordedAtDesc(SensorDevice sensorDevice, Pageable pageable);
    List<SensorReading> findByPlantOrderByRecordedAtDesc(Plant plant, Pageable pageable);
    List<SensorReading> findByPlantIdOrderByRecordedAtDesc(Long plantId, Pageable pageable);

    @Query("SELECT r FROM SensorReading r WHERE r.plant.id = :plantId ORDER BY r.recordedAt DESC LIMIT 1")
    Optional<SensorReading> findLatestByPlantId(@Param("plantId") Long plantId);

    @Query("SELECT r FROM SensorReading r WHERE r.sensorDevice.deviceId = :deviceId ORDER BY r.recordedAt DESC LIMIT 1")
    Optional<SensorReading> findLatestByDeviceId(@Param("deviceId") String deviceId);
}
