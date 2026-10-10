package com.myplant.controller;

import com.myplant.dto.CareRecommendationDTO;
import com.myplant.dto.DiseaseInspectionDTO;
import com.myplant.dto.WeatherDTO;
import com.myplant.entity.*;
import com.myplant.repository.*;
import com.myplant.security.JwtTokenProvider;
import com.myplant.service.GroqAIService;
import com.myplant.service.WeatherService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * PlantAssistantController - Endpoints for Groq-powered contextual AI intelligence:
 * - Natural-language plant assistant chat
 * - Evidence-grounded care recommendations
 * - Disease observation assessment
 * - Concise health summaries
 * - Groq service status
 */
@RestController
@RequestMapping("/api")
@AllArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174"})
@Slf4j
public class PlantAssistantController {

    private final GroqAIService groqAIService;
    private final PlantRepository plantRepository;
    private final UserRepository userRepository;
    private final SensorReadingRepository sensorReadingRepository;
    private final CareRecommendationRepository careRecommendationRepository;
    private final DiseaseInspectionRepository diseaseInspectionRepository;
    private final WeatherService weatherService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Check Groq API configuration & operational status
     */
    @GetMapping("/ai/groq/status")
    public ResponseEntity<Map<String, Object>> getGroqStatus() {
        return ResponseEntity.ok(groqAIService.getServiceStatus());
    }

    /**
     * Interactive Natural-Language Plant Assistant
     */
    @PostMapping("/plants/{plantId}/assistant")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> askAssistant(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long plantId,
            @RequestBody Map<String, Object> body) {

        String token = jwtTokenProvider.extractTokenFromBearerToken(authorization);
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        Plant plant = plantRepository.findById(plantId)
                .orElseThrow(() -> new RuntimeException("Plant not found: " + plantId));
        if (!plant.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: You do not own this plant.");
        }

        String question = (String) body.get("question");
        if (question == null || question.trim().isEmpty()) {
            throw new IllegalArgumentException("Question cannot be blank.");
        }

        // Get latest sensor reading if available
        Optional<SensorReading> latestReading = sensorReadingRepository.findLatestByPlantId(plantId);

        // Get plant weather
        WeatherDTO weather = (plant.getLatitude() != null && plant.getLongitude() != null)
                ? weatherService.getWeatherForCoordinates(plant.getLatitude(), plant.getLongitude(), plant.getLocation())
                : weatherService.getWeatherForCity(plant.getUser().getCity());

        @SuppressWarnings("unchecked")
        List<Map<String, String>> history = (List<Map<String, String>>) body.getOrDefault("history", Collections.emptyList());

        String answer = groqAIService.askPlantAssistant(
                plant,
                latestReading.orElse(null),
                weather,
                question,
                history
        );

        Map<String, Object> response = new HashMap<>();
        response.put("answer", answer);
        response.put("plantId", plantId);
        response.put("plantName", plant.getName());
        response.put("respondedAt", LocalDateTime.now());
        response.put("provider", groqAIService.isConfigured() ? "Groq LLM (" + groqAIService.getServiceStatus().get("model") + ")" : "Botanical Intelligence Rule Engine");

        return ResponseEntity.ok(response);
    }

    /**
     * Get or regenerate evidence-grounded care recommendations
     */
    @GetMapping("/plants/{plantId}/recommendations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CareRecommendationDTO>> getRecommendations(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long plantId) {

        String token = jwtTokenProvider.extractTokenFromBearerToken(authorization);
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        Plant plant = plantRepository.findById(plantId)
                .orElseThrow(() -> new RuntimeException("Plant not found: " + plantId));
        if (!plant.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: You do not own this plant.");
        }

        // Retrieve existing recommendations
        List<CareRecommendation> existing = careRecommendationRepository.findByPlantIdOrderByGeneratedAtDesc(plantId);

        // If none exist or older than 6 hours, generate fresh recommendations with Groq
        if (existing.isEmpty() || existing.get(0).getGeneratedAt().isBefore(LocalDateTime.now().minusHours(6))) {
            Optional<SensorReading> latestReading = sensorReadingRepository.findLatestByPlantId(plantId);
            WeatherDTO weather = (plant.getLatitude() != null && plant.getLongitude() != null)
                    ? weatherService.getWeatherForCoordinates(plant.getLatitude(), plant.getLongitude(), plant.getLocation())
                    : weatherService.getWeatherForCity(plant.getUser().getCity());

            List<Map<String, Object>> freshList = groqAIService.generateCareRecommendations(
                    plant,
                    latestReading.orElse(null),
                    weather
            );

            // Persist new recommendations
            careRecommendationRepository.deleteByPlantId(plantId);
            List<CareRecommendation> savedList = new ArrayList<>();
            for (Map<String, Object> item : freshList) {
                CareRecommendation rec = new CareRecommendation();
                rec.setPlant(plant);
                rec.setUser(plant.getUser());
                rec.setCategory((String) item.getOrDefault("category", "CARE"));
                rec.setTitle((String) item.getOrDefault("title", "Plant Care Advisory"));
                rec.setMessage((String) item.getOrDefault("message", ""));
                rec.setRecommendedAction((String) item.getOrDefault("recommendedAction", ""));
                rec.setEvidence((String) item.getOrDefault("evidence", ""));
                rec.setPriority((String) item.getOrDefault("priority", "MEDIUM"));
                rec.setConfidence(item.get("confidence") != null ? Double.parseDouble(item.get("confidence").toString()) : 0.90);
                rec.setGeneratedAt(LocalDateTime.now());
                rec.setValidUntil(LocalDateTime.now().plusDays(1));
                savedList.add(careRecommendationRepository.save(rec));
            }
            existing = savedList;
        }

        List<CareRecommendationDTO> dtos = existing.stream().map(this::toRecommendationDTO).toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * Submit disease observation and get structured assessment
     */
    @PostMapping("/plants/{plantId}/disease-inspections")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DiseaseInspectionDTO> inspectDisease(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long plantId,
            @RequestBody Map<String, String> body) {

        String token = jwtTokenProvider.extractTokenFromBearerToken(authorization);
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        Plant plant = plantRepository.findById(plantId)
                .orElseThrow(() -> new RuntimeException("Plant not found: " + plantId));
        if (!plant.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: You do not own this plant.");
        }

        String symptoms = body.get("symptoms");
        String observationNotes = body.get("notes");
        String imageUrl = body.get("imageUrl");

        Map<String, Object> assessment = groqAIService.explainDiseaseObservation(plant, symptoms, observationNotes);

        DiseaseInspection inspection = new DiseaseInspection();
        inspection.setPlant(plant);
        inspection.setUser(plant.getUser());
        inspection.setImageUrl(imageUrl);
        inspection.setSymptoms((String) assessment.getOrDefault("observedSymptoms", symptoms != null ? symptoms : "Symptoms observed"));
        inspection.setPossibleCauses((String) assessment.getOrDefault("possibleCauses", "Moisture or nutrient stress"));
        inspection.setAlternativeExplanations((String) assessment.getOrDefault("alternativeExplanations", "Lighting condition imbalance"));
        inspection.setConfidence(assessment.get("confidence") != null ? Double.parseDouble(assessment.get("confidence").toString()) : 0.85);
        inspection.setRecommendedInspection((String) assessment.getOrDefault("recommendedInspection", "Examine root aeration"));
        inspection.setPreventiveMeasures((String) assessment.getOrDefault("preventiveMeasures", "Optimize drainage"));
        inspection.setNextSteps((String) assessment.getOrDefault("nextSteps", "Monitor foliage over next 48 hours"));
        inspection.setInspectedAt(LocalDateTime.now());

        DiseaseInspection saved = diseaseInspectionRepository.save(inspection);
        return ResponseEntity.ok(toDiseaseDTO(saved));
    }

    /**
     * Get inspection history for plant
     */
    @GetMapping("/plants/{plantId}/disease-inspections")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DiseaseInspectionDTO>> getPlantDiseaseHistory(
            @PathVariable Long plantId) {

        List<DiseaseInspection> list = diseaseInspectionRepository.findByPlantIdOrderByInspectedAtDesc(plantId);
        return ResponseEntity.ok(list.stream().map(this::toDiseaseDTO).toList());
    }

    /**
     * Concise plant health summary
     */
    @GetMapping("/plants/{plantId}/health-summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getHealthSummary(
            @PathVariable Long plantId) {

        Plant plant = plantRepository.findById(plantId)
                .orElseThrow(() -> new RuntimeException("Plant not found: " + plantId));

        Optional<SensorReading> latestReading = sensorReadingRepository.findLatestByPlantId(plantId);
        WeatherDTO weather = (plant.getLatitude() != null && plant.getLongitude() != null)
                ? weatherService.getWeatherForCoordinates(plant.getLatitude(), plant.getLongitude(), plant.getLocation())
                : weatherService.getWeatherForCity(plant.getUser().getCity());

        String summary = groqAIService.generateHealthSummary(plant, latestReading.orElse(null), weather);

        Map<String, Object> response = new HashMap<>();
        response.put("summary", summary);
        response.put("plantId", plantId);
        response.put("health", plant.getHealth());
        response.put("hasSensor", latestReading.isPresent() && !latestReading.get().isStale());
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    /**
     * Live weather for specific plant's location
     */
    @GetMapping("/plants/{plantId}/weather")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WeatherDTO> getPlantWeather(
            @PathVariable Long plantId) {

        Plant plant = plantRepository.findById(plantId)
                .orElseThrow(() -> new RuntimeException("Plant not found: " + plantId));

        WeatherDTO weather = (plant.getLatitude() != null && plant.getLongitude() != null)
                ? weatherService.getWeatherForCoordinates(plant.getLatitude(), plant.getLongitude(), plant.getLocation())
                : weatherService.getWeatherForCity(plant.getUser().getCity());

        return ResponseEntity.ok(weather);
    }

    private CareRecommendationDTO toRecommendationDTO(CareRecommendation r) {
        CareRecommendationDTO dto = new CareRecommendationDTO();
        dto.setId(r.getId());
        dto.setPlantId(r.getPlant().getId());
        dto.setCategory(r.getCategory());
        dto.setTitle(r.getTitle());
        dto.setMessage(r.getMessage());
        dto.setRecommendedAction(r.getRecommendedAction());
        dto.setEvidence(r.getEvidence());
        dto.setPriority(r.getPriority());
        dto.setConfidence(r.getConfidence());
        dto.setGeneratedAt(r.getGeneratedAt());
        dto.setValidUntil(r.getValidUntil());
        return dto;
    }

    private DiseaseInspectionDTO toDiseaseDTO(DiseaseInspection d) {
        DiseaseInspectionDTO dto = new DiseaseInspectionDTO();
        dto.setId(d.getId());
        dto.setPlantId(d.getPlant().getId());
        dto.setImageUrl(d.getImageUrl());
        dto.setSymptoms(d.getSymptoms());
        dto.setPossibleCauses(d.getPossibleCauses());
        dto.setAlternativeExplanations(d.getAlternativeExplanations());
        dto.setConfidence(d.getConfidence());
        dto.setRecommendedInspection(d.getRecommendedInspection());
        dto.setPreventiveMeasures(d.getPreventiveMeasures());
        dto.setNextSteps(d.getNextSteps());
        dto.setInspectedAt(d.getInspectedAt());
        return dto;
    }
}
