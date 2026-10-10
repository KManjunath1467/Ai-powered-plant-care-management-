package com.myplant.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DiseaseInspection Entity - Records AI visual plant disease assessments and inspections
 */
@Entity
@Table(name = "disease_inspections", indexes = {
    @Index(name = "idx_disease_plant", columnList = "plant_id"),
    @Index(name = "idx_disease_user", columnList = "user_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DiseaseInspection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id", nullable = false)
    @JsonIgnoreProperties({"user", "diseaseInspections", "sensorDevice"})
    private Plant plant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"plants", "notifications", "password"})
    private User user;

    private String imageUrl;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String symptoms;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String possibleCauses;

    @Column(columnDefinition = "TEXT")
    private String alternativeExplanations;

    private Double confidence = 0.85;

    @Column(columnDefinition = "TEXT")
    private String recommendedInspection;

    @Column(columnDefinition = "TEXT")
    private String preventiveMeasures;

    @Column(columnDefinition = "TEXT")
    private String nextSteps;

    @Column(nullable = false)
    private LocalDateTime inspectedAt = LocalDateTime.now();
}
