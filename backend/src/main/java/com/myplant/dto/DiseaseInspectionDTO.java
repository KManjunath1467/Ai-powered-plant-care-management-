package com.myplant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiseaseInspectionDTO {
    private Long id;
    private Long plantId;
    private String imageUrl;
    private String symptoms;
    private String possibleCauses;
    private String alternativeExplanations;
    private Double confidence;
    private String recommendedInspection;
    private String preventiveMeasures;
    private String nextSteps;
    private LocalDateTime inspectedAt;
}
