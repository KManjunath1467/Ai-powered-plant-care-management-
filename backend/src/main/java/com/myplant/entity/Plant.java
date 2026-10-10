package com.myplant.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Plant Entity - Augmented with botanical taxonomy, location coordinates,
 * anatomy information, and IoT sensor device integration.
 */
@Entity
@Table(name = "plants")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Plant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Reference to owner
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"plants", "notifications", "password"})
    private User user;

    // Plant Name
    @Column(nullable = false)
    private String name;

    // Plant Type / Common Name
    @Column(nullable = false)
    private String plantType;

    // Scientific Botanical Name (e.g. "Sansevieria trifasciata")
    private String scientificName;

    // Botanical Family (e.g. "Asparagaceae")
    private String plantFamily;

    // Pot Size
    @Column(nullable = false)
    private String potSize;

    // Indoor / Outdoor
    @Column(nullable = false)
    private Boolean isIndoor;

    // Plant Location Name (e.g. "Living Room South Window")
    @Column(nullable = false)
    private String location;

    // Location coordinates for specific micro-climate weather
    private Double latitude;
    private Double longitude;

    // Last Watered Date
    private LocalDate lastWateredDate;

    // Watering Streak
    @Column(nullable = false)
    private Integer wateringStreak = 0;

    // Plant Health Status ("Healthy", "Needs Attention", "Under-watered", etc.)
    @Column(nullable = false)
    private String health = "Healthy";

    // Notes
    @Column(columnDefinition = "TEXT")
    private String notes;

    // Structured Anatomy Explanation JSON (roots, stem, leaves, flowers, fruits, vascular tissues)
    @Column(columnDefinition = "LONGTEXT")
    private String anatomyJson;

    // Custom Watering Interval
    private Integer customWateringInterval;

    // Image URL
    @Column(name = "image_url")
    private String imageUrl;

    // Created At
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Updated At
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Paired IoT Sensor Device (null if no sensor rod paired)
    @OneToOne(mappedBy = "plant", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"plant", "user"})
    private SensorDevice sensorDevice;

    // Watering History
    @OneToMany(
            mappedBy = "plant",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @JsonIgnore
    private List<WateringHistory> wateringHistory;

    // Care Recommendations
    @OneToMany(
            mappedBy = "plant",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @JsonIgnore
    private List<CareRecommendation> careRecommendations;

    // Disease Inspections
    @OneToMany(
            mappedBy = "plant",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @JsonIgnore
    private List<DiseaseInspection> diseaseInspections;

    // Plant Care Rule
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_care_rule_id")
    @JsonIgnoreProperties({"plants"})
    private PlantCareRule plantCareRule;
}