package com.myplant.repository;

import com.myplant.entity.DiseaseInspection;
import com.myplant.entity.Plant;
import com.myplant.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiseaseInspectionRepository extends JpaRepository<DiseaseInspection, Long> {
    List<DiseaseInspection> findByPlantOrderByInspectedAtDesc(Plant plant);
    List<DiseaseInspection> findByPlantIdOrderByInspectedAtDesc(Long plantId);
    List<DiseaseInspection> findByUserOrderByInspectedAtDesc(User user);
}
