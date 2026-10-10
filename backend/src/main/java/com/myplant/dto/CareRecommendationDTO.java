package com.myplant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CareRecommendationDTO {
    private Long id;
    private Long plantId;
    private String category;
    private String title;
    private String message;
    private String recommendedAction;
    private String evidence;
    private String priority;
    private Double confidence;
    private LocalDateTime generatedAt;
    private LocalDateTime validUntil;
}
