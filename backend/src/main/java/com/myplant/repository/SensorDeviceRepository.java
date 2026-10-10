package com.myplant.repository;

import com.myplant.entity.Plant;
import com.myplant.entity.SensorDevice;
import com.myplant.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SensorDeviceRepository extends JpaRepository<SensorDevice, Long> {
    Optional<SensorDevice> findByDeviceId(String deviceId);
    Optional<SensorDevice> findByDeviceIdAndPairingToken(String deviceId, String pairingToken);
    Optional<SensorDevice> findByPlant(Plant plant);
    Optional<SensorDevice> findByPlantId(Long plantId);
    List<SensorDevice> findByUser(User user);
    List<SensorDevice> findByUserId(Long userId);
    List<SensorDevice> findByStatus(SensorDevice.DeviceStatus status);
}
