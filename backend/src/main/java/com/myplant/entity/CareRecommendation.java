package com.myplant.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * CareRecommendation Entity - AI/Rule generated structured recommendations
 */
@Entity
@Table(name = "care_recommendations", indexes = {
    @Index(name = "idx_rec_plant", columnList = "plant_id"),
    @Index(name = "idx_rec_user", columnList = "user_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CareRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id", nullable = false)
    @JsonIgnoreProperties({"user", "careRecommendations", "sensorDevice"})
    private Plant plant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"plants", "notifications", "password"})
    private User user;

    @Column(nullable = false)
    private String category; // WATERING, SUNLIGHT, SOIL, WEATHER, GENERAL

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(columnDefinition = "TEXT")
    private String recommendedAction;

    @Column(columnDefinition = "TEXT")
    private String evidence;

    @Column(nullable = false)
    private String priority = "MEDIUM"; // LOW, MEDIUM, HIGH, CRITICAL

    private Double confidence = 0.90;

    @Column(nullable = false)
    private LocalDateTime generatedAt = LocalDateTime.now();

    private LocalDateTime validUntil;
}
