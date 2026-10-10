package com.myplant.service;

import com.myplant.dto.PlantCareRuleDTO;
import com.myplant.entity.PlantCareRule;
import com.myplant.repository.PlantCareRuleRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

/** Plant identification based on actual image bytes, followed by Groq explanations. */
@Service
@AllArgsConstructor
@Slf4j
public class AIAnalysisService {
    private final PlantCareRuleRepository plantCareRuleRepository;
    private final GroqAIService groqAIService;

    public Map<String, Object> analyzePlantImage(MultipartFile image, String plantName, Long userId) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Upload a plant photograph first.");
        }
        if (image.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("Image must be 10 MB or smaller.");
        }
        String mime = image.getContentType();
        if (mime == null || !List.of("image/jpeg", "image/png", "image/webp").contains(mime.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Unsupported image type. Upload JPEG, PNG, or WebP.");
        }

        final Map<String, Object> identification;
        try {
            identification = groqAIService.identifyPlantFromImage(image.getBytes(), mime, plantName);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Could not read the uploaded image.", ex);
        }

        String candidateName = stringValue(identification.get("commonName"), "Unknown plant");
        String scientificName = stringValue(identification.get("scientificName"), "Unknown");
        String plantFamily = stringValue(identification.get("plantFamily"), "Unknown");
        if (candidateName.equalsIgnoreCase("Unknown") || candidateName.equalsIgnoreCase("Unknown plant")) {
            throw new IllegalArgumentException("The image did not contain a confidently identifiable plant. Try a clear photo of the leaves, flowers, or whole plant.");
        }

        Map<String, Object> taxonomy = new HashMap<>(identification);
        Map<String, Object> anatomy = groqAIService.generatePlantAnatomy(candidateName, scientificName);
        Optional<PlantCareRule> ruleOpt = plantCareRuleRepository.findByPlantNameIgnoreCase(candidateName);
        PlantCareRule rule = ruleOpt.orElseGet(() -> createDefaultRule(candidateName));

        Map<String, Object> care = new HashMap<>();
        care.put("wateringSchedule", valueOr(identification.get("wateringNeeds"), rule.getWateringFrequency()));
        care.put("sunlightNeeds", valueOr(identification.get("sunlightNeeds"), rule.getSunlightNeeds()));
        care.put("humidityPreference", valueOr(identification.get("humidityNeeds"), rule.getHumidityPreference()));
        care.put("temperatureRange", rule.getTemperatureRange());
        care.put("soilPreferences", valueOr(identification.get("soilPreferences"), "Not established; verify for this species"));
        care.put("toxicity", valueOr(identification.get("toxicityInfo"), "Unknown; verify before allowing pets or children to access the plant"));

        Map<String, Object> health = new HashMap<>();
        health.put("status", "NOT_ASSESSED");
        health.put("message", "Plant identity was analysed, but plant health has not been assessed. Upload a clear image of affected leaves or connect sensors for a separate assessment.");
        health.put("issues", Collections.emptyList());

        Map<String, Object> result = new HashMap<>();
        result.put("candidateName", candidateName);
        result.put("scientificName", scientificName);
        result.put("plantFamily", plantFamily);
        result.put("taxonomy", taxonomy);
        result.put("anatomy", anatomy);
        result.put("confidence", valueOr(identification.get("identificationConfidence"), "unknown"));
        result.put("confidenceReason", valueOr(identification.get("confidenceReason"), "No confidence explanation provided by the identification model."));
        result.put("visibleObservations", valueOr(identification.get("visibleObservations"), "No observations returned."));
        result.put("detectedPlant", convertToDTO(rule));
        result.put("healthAnalysis", health);
        result.put("careRecommendations", care);
        result.put("tips", rule.getCareTips());
        result.put("groqStatus", groqAIService.getServiceStatus());
        result.put("imageAnalysed", true);
        return result;
    }

    private String stringValue(Object value, String fallback) {
        return value == null || value.toString().isBlank() ? fallback : value.toString().trim();
    }

    private Object valueOr(Object value, Object fallback) {
        return value == null || value.toString().isBlank() ? fallback : value;
    }

    private PlantCareRule createDefaultRule(String name) {
        PlantCareRule rule = new PlantCareRule();
        rule.setPlantName(name);
        rule.setBaseWateringDays(7);
        rule.setWateringFrequency("Check soil moisture before watering; species-specific interval not verified");
        rule.setSunlightNeeds("Verify species-specific light requirements");
        rule.setHumidityPreference("Not established");
        rule.setTemperatureRange("Not established");
        rule.setDifficultyLevel("Unknown");
        rule.setDescription("Care information should be verified for this identification.");
        rule.setRecommendedPotSize("Not established");
        rule.setGrowthRate("Unknown");
        rule.setCareTips("Check soil moisture and verify care advice using a reliable botanical source.");
        return rule;
    }

    private PlantCareRuleDTO convertToDTO(PlantCareRule rule) {
        PlantCareRuleDTO dto = new PlantCareRuleDTO();
        dto.setId(rule.getId());
        dto.setPlantName(rule.getPlantName());
        dto.setBaseWateringDays(rule.getBaseWateringDays());
        dto.setWateringFrequency(rule.getWateringFrequency());
        dto.setSunlightNeeds(rule.getSunlightNeeds());
        dto.setHumidityPreference(rule.getHumidityPreference());
        dto.setTemperatureRange(rule.getTemperatureRange());
        dto.setDifficultyLevel(rule.getDifficultyLevel());
        dto.setDescription(rule.getDescription());
        dto.setCommonIssues(rule.getCommonIssues());
        dto.setCareTips(rule.getCareTips());
        dto.setRecommendedPotSize(rule.getRecommendedPotSize());
        dto.setGrowthRate(rule.getGrowthRate());
        dto.setMaxSize(rule.getMaxSize());
        return dto;
    }
}
