package com.myplant.repository;

import com.myplant.entity.CareRecommendation;
import com.myplant.entity.Plant;
import com.myplant.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CareRecommendationRepository extends JpaRepository<CareRecommendation, Long> {
    List<CareRecommendation> findByPlantOrderByGeneratedAtDesc(Plant plant);
    List<CareRecommendation> findByPlantIdOrderByGeneratedAtDesc(Long plantId);
    List<CareRecommendation> findByUserOrderByGeneratedAtDesc(User user);
    List<CareRecommendation> findByUserIdOrderByGeneratedAtDesc(Long userId);
    void deleteByPlantId(Long plantId);
}
