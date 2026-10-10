package com.myplant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myplant.dto.WeatherDTO;
import com.myplant.entity.Plant;
import com.myplant.entity.SensorReading;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GroqAIServiceTest {

    private GroqAIService groqAIService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        groqAIService = new GroqAIService(objectMapper);
    }

    @Test
    @DisplayName("Verify Groq unconfigured status and botanical fallback")
    void testGroqStatusUnconfigured() {
        Map<String, Object> status = groqAIService.getServiceStatus();
        assertNotNull(status);
        assertFalse((Boolean) status.get("configured"));
        assertEquals("Groq Cloud API", status.get("provider"));
    }

    @Test
    @DisplayName("Verify structured plant identification output with taxonomy fallback")
    void testPlantIdentification() {
        Map<String, Object> result = groqAIService.explainPlantIdentification("Monstera", "Healthy broad leaves with fenestrations");
        assertNotNull(result);
        assertEquals("Monstera", result.get("commonName"));
        assertTrue(result.containsKey("scientificName"));
        assertTrue(result.containsKey("plantFamily"));
        assertTrue(result.containsKey("sunlightNeeds"));
        assertTrue(result.containsKey("wateringNeeds"));
        assertTrue(result.containsKey("anatomyOverview"));
    }

    @Test
    @DisplayName("Verify structured botanical anatomy output includes all essential organs")
    void testPlantAnatomyBreakdown() {
        Map<String, Object> anatomy = groqAIService.generatePlantAnatomy("Snake Plant", "Sansevieria trifasciata");
        assertNotNull(anatomy);
        assertTrue(anatomy.containsKey("roots"));
        assertTrue(anatomy.containsKey("stem"));
        assertTrue(anatomy.containsKey("leaves"));
        assertTrue(anatomy.containsKey("vascularSystem"));
        assertTrue(anatomy.containsKey("budsAndNewGrowth"));

        @SuppressWarnings("unchecked")
        Map<String, Object> vascular = (Map<String, Object>) anatomy.get("vascularSystem");
        assertNotNull(vascular);
        assertTrue(vascular.containsKey("xylemFunction"));
        assertTrue(vascular.containsKey("phloemFunction"));
    }

    @Test
    @DisplayName("Verify care recommendations adhere to verified evidence")
    void testCareRecommendationsEvidence() {
        Plant plant = new Plant();
        plant.setId(1L);
        plant.setName("Living Room Pothos");
        plant.setPlantType("Pothos");
        plant.setIsIndoor(true);

        SensorReading reading = new SensorReading();
        reading.setSoilMoisture(22.0); // Dry soil
        reading.setSoilTemperature(21.5);
        reading.setRecordedAt(LocalDateTime.now());

        WeatherDTO weather = new WeatherDTO();
        weather.setCity("Seattle");
        weather.setTemperature(18.0);
        weather.setHumidity(65.0);
        weather.setIsRaining(false);

        List<Map<String, Object>> recs = groqAIService.generateCareRecommendations(plant, reading, weather);
        assertNotNull(recs);
        assertFalse(recs.isEmpty());

        boolean hasWateringRec = recs.stream()
                .anyMatch(r -> "WATERING".equals(r.get("category")));
        assertTrue(hasWateringRec, "Expected watering recommendation for low soil moisture");
    }

    @Test
    @DisplayName("Verify assistant answers questions strictly using plant context")
    void testAssistantQuestionAnswering() {
        Plant plant = new Plant();
        plant.setId(2L);
        plant.setName("Kitchen Basil");
        plant.setPlantType("Basil");
        plant.setIsIndoor(true);
        plant.setLocation("Window sill");

        String answer = groqAIService.askPlantAssistant(plant, null, null, "Should I water today?", List.of());
        assertNotNull(answer);
        assertTrue(answer.toLowerCase().contains("basil") || answer.toLowerCase().contains("water"));
    }
}
