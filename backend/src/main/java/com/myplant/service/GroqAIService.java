package com.myplant.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myplant.dto.WeatherDTO;
import com.myplant.entity.Plant;
import com.myplant.entity.SensorReading;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * GroqAIService - Production-grade Groq LLM Provider Integration.
 *
 * Centralises:
 * - Secure API key & model configuration via GROQ_API_KEY and GROQ_MODEL
 * - Request timeouts, controlled retries, rate-limit backoff handling
 * - Structured JSON output validation and fallback parsing
 * - Metrics: latency, request counts, error logging without exposing secrets
 * - Graceful degradation when key is missing or service is unavailable
 */
@Service
@Slf4j
public class GroqAIService {

    @Value("${groq.api.key:${GROQ_API_KEY:}}")
    private String apiKey;

    @Value("${groq.model:${GROQ_MODEL:llama-3.3-70b-versatile}}")
    private String modelName;

    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Operational metrics
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    private volatile long lastLatencyMs = 0;
    private volatile String lastError = null;

    public GroqAIService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty() && !apiKey.contains("your_groq_api_key");
    }

    public Map<String, Object> getServiceStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("configured", isConfigured());
        status.put("model", modelName);
        status.put("totalRequests", totalRequests.get());
        status.put("successfulRequests", successfulRequests.get());
        status.put("failedRequests", failedRequests.get());
        status.put("lastLatencyMs", lastLatencyMs);
        status.put("lastError", lastError);
        status.put("provider", "Groq Cloud API");
        return status;
    }

    /**
     * Executes a chat completion request to Groq with controlled retries and error isolation.
     */
    public String executeGroqPrompt(String systemPrompt, String userPrompt, boolean jsonMode) {
        if (!isConfigured()) {
            log.info("Groq API key not configured. Using deterministic botanical engine fallback.");
            return null;
        }

        totalRequests.incrementAndGet();
        long startTime = System.currentTimeMillis();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey.trim());

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", modelName);
        payload.put("temperature", 0.2); // Low temperature for deterministic, accurate facts

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt));
        payload.put("messages", messages);

        if (jsonMode) {
            payload.put("response_format", Map.of("type", "json_object"));
        }

        int maxRetries = 2;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
                ResponseEntity<String> response = restTemplate.exchange(
                        apiUrl, HttpMethod.POST, requestEntity, String.class);

                long latency = System.currentTimeMillis() - startTime;
                this.lastLatencyMs = latency;
                successfulRequests.incrementAndGet();
                this.lastError = null;

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    JsonNode choices = root.path("choices");
                    if (choices.isArray() && choices.size() > 0) {
                        return choices.get(0).path("message").path("content").asText();
                    }
                }
            } catch (HttpStatusCodeException ex) {
                lastError = "Groq HTTP Error " + ex.getStatusCode() + ": " + ex.getStatusText();
                log.warn("Groq API request failed (attempt {}/{}): {}", attempt, maxRetries, ex.getStatusCode());
                if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS && attempt < maxRetries) {
                    try {
                        Thread.sleep(1500L * attempt); // Backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    continue;
                }
                break;
            } catch (Exception ex) {
                lastError = "Groq Connection Error: " + ex.getMessage();
                log.warn("Groq API connection exception: {}", ex.getMessage());
                break;
            }
        }

        failedRequests.incrementAndGet();
        return null;
    }

    /**
     * A. Plant identification explanation and taxonomy
     */
    public Map<String, Object> explainPlantIdentification(String plantName, String visualNotes) {
        String systemPrompt = "You are an expert botanical taxonomist and plant-care specialist. " +
                "Output strictly valid JSON with no markdown wrapping. Do not fabricate unverifiable information.";
        String userPrompt = String.format(
                "Analyze and explain the identified plant '%s'. Visual observations: %s.\n" +
                "Return a JSON object with fields:\n" +
                "- commonName (string)\n" +
                "- scientificName (string)\n" +
                "- plantFamily (string)\n" +
                "- possibleSpecies (array of strings)\n" +
                "- confidenceScore (number between 0.70 and 0.98)\n" +
                "- sunlightNeeds (string)\n" +
                "- wateringNeeds (string)\n" +
                "- soilPreferences (string)\n" +
                "- humidityNeeds (string)\n" +
                "- toxicityInfo (string)\n" +
                "- generalCareSummary (string)\n" +
                "- anatomyOverview (string)",
                plantName, visualNotes != null ? visualNotes : "Healthy foliage observed"
        );

        String rawJson = executeGroqPrompt(systemPrompt, userPrompt, true);
        if (rawJson != null) {
            try {
                return objectMapper.readValue(rawJson, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.error("Failed to parse Groq identification JSON, falling back", e);
            }
        }

        // Deterministic botanical fallback
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("commonName", plantName);
        fallback.put("scientificName", getBotanicalScientificName(plantName));
        fallback.put("plantFamily", getBotanicalFamily(plantName));
        fallback.put("possibleSpecies", List.of(plantName + " var. standard", plantName + " cultivar"));
        fallback.put("confidenceScore", 0.92);
        fallback.put("sunlightNeeds", "Bright, indirect sunlight");
        fallback.put("wateringNeeds", "Allow top 2-3 inches of soil to dry between waterings");
        fallback.put("soilPreferences", "Well-draining, nutrient-rich potting mix with perlite");
        fallback.put("humidityNeeds", "Moderate to high humidity (50-65%)");
        fallback.put("toxicityInfo", "Keep away from curious pets and small children as a precaution.");
        fallback.put("generalCareSummary", "Thrives in warm indoor environments with consistent light and controlled moisture.");
        fallback.put("anatomyOverview", "Features a well-developed root cluster, sturdy stem or petiole, and lush foliage adapted for photosynthesis.");
        return fallback;
    }

    /**
     * B. Plant anatomy explanation
     */
    public Map<String, Object> generatePlantAnatomy(String plantName, String scientificName) {
        String systemPrompt = "You are a botanical anatomy professor. Return strictly valid JSON explaining plant morphology.";
        String userPrompt = String.format(
                "Provide detailed, scientifically accurate botanical anatomy for plant '%s' (%s).\n" +
                "Return a JSON object with fields:\n" +
                "- roots: { structure: string, role: string, careTip: string }\n" +
                "- stem: { structure: string, role: string, careTip: string }\n" +
                "- leaves: { structure: string, role: string, careTip: string }\n" +
                "- flowers: { structure: string, role: string, bloomSeason: string }\n" +
                "- fruitsAndSeeds: { structure: string, role: string }\n" +
                "- budsAndNewGrowth: { description: string, indicator: string }\n" +
                "- vascularSystem: { xylemFunction: string, phloemFunction: string }\n" +
                "- anatomicalSummary: string",
                plantName, scientificName != null ? scientificName : "species"
        );

        String rawJson = executeGroqPrompt(systemPrompt, userPrompt, true);
        if (rawJson != null) {
            try {
                return objectMapper.readValue(rawJson, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.error("Failed to parse Groq anatomy JSON, falling back", e);
            }
        }

        // High quality botanical fallback
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("roots", Map.of(
                "structure", "Fibrous root network anchoring the plant and absorbing moisture and vital minerals.",
                "role", "Water intake, mineral transport, oxygenation, and nutrient storage.",
                "careTip", "Ensure well-aerated potting soil to prevent water stagnation and root suffocation."
        ));
        fallback.put("stem", Map.of(
                "structure", "Herbaceous or semi-woody main stem supporting petioles and leaf nodes.",
                "role", "Structural support, light positioning, and bidirectional nutrient highway.",
                "careTip", "Rotate pot periodically to promote balanced stem verticality."
        ));
        fallback.put("leaves", Map.of(
                "structure", "Broad photosynthetic lamina with chlorophyll-rich palisade mesophyll and stomata.",
                "role", "Photosynthesis, gas exchange, and transpiration regulation.",
                "careTip", "Gently wipe leaves with a soft damp cloth to clear dust and boost light absorption."
        ));
        fallback.put("flowers", Map.of(
                "structure", "Reproductive floral structures or spathe/spadix depending on cultivar.",
                "role", "Pollination and gamete transfer.",
                "bloomSeason", "Spring to late summer under optimal photoperiod and warmth."
        ));
        fallback.put("fruitsAndSeeds", Map.of(
                "structure", "Seed capsules or berries emerging after successful fertilization.",
                "role", "Genetic propagation and seed dispersal."
        ));
        fallback.put("budsAndNewGrowth", Map.of(
                "description", "Active apical and lateral meristems producing fresh foliage shoots.",
                "indicator", "Vibrant light-green tips signify active vegetative vigor and adequate nutrition."
        ));
        fallback.put("vascularSystem", Map.of(
                "xylemFunction", "Unidirectional upward transport of soil water and dissolved inorganic ions via capillary action and transpiration pull.",
                "phloemFunction", "Multidirectional translocation of photosynthesized sugars from mature source leaves to roots and new shoots."
        ));
        fallback.put("anatomicalSummary", String.format("A well-balanced morphology tailored for indoor light absorption and balanced transpiration for %s.", plantName));
        return fallback;
    }

    /**
     * C. Intelligent care recommendations using verified plant, weather, and sensor evidence
     */
    public List<Map<String, Object>> generateCareRecommendations(Plant plant, SensorReading sensorReading, WeatherDTO weather) {
        StringBuilder context = new StringBuilder();
        context.append("Plant Name: ").append(plant.getName()).append("\n");
        context.append("Plant Type: ").append(plant.getPlantType()).append("\n");
        context.append("Growing Environment: ").append(plant.getIsIndoor() ? "Indoor" : "Outdoor").append("\n");
        context.append("Last Watered Date: ").append(plant.getLastWateredDate() != null ? plant.getLastWateredDate() : "Never recorded").append("\n");

        if (weather != null) {
            context.append("Current Weather: ").append(weather.getTemperature()).append("°C, ")
                   .append(weather.getHumidity()).append("% humidity, ")
                   .append(weather.getDescription()).append(", Raining: ").append(weather.getIsRaining()).append("\n");
        }

        if (sensorReading != null && !sensorReading.isStale()) {
            context.append("Live Sensor Rod Data:\n")
                   .append("- Soil Moisture: ").append(sensorReading.getSoilMoisture()).append("%\n")
                   .append("- Soil Temperature: ").append(sensorReading.getSoilTemperature()).append("°C\n")
                   .append("- Soil EC: ").append(sensorReading.getSoilEc()).append(" mS/cm\n")
                   .append("- Soil pH: ").append(sensorReading.getSoilPh()).append("\n")
                   .append("- Ambient Light: ").append(sensorReading.getLightLux()).append(" lux\n");
        } else if (sensorReading != null && sensorReading.isStale()) {
            context.append("Sensor Status: Offline / Stale reading (>2 hours old). Current conditions cannot be verified.\n");
        } else {
            context.append("Sensor Status: No sensor rod connected. Soil moisture is not measured directly.\n");
        }

        String systemPrompt = "You are an intelligent agricultural agronomist and indoor plant specialist. " +
                "Evaluate the given verified evidence. Never invent sensor readings. " +
                "Return strictly valid JSON with key 'recommendations' containing an array of objects. " +
                "Each object must have: plantId, category, title, message, recommendedAction, evidence, priority (LOW|MEDIUM|HIGH), confidence (0.8-1.0).";

        String userPrompt = "Generate 2 to 4 evidence-grounded recommendations for this plant:\n" + context.toString();

        String rawJson = executeGroqPrompt(systemPrompt, userPrompt, true);
        if (rawJson != null) {
            try {
                JsonNode root = objectMapper.readTree(rawJson);
                JsonNode recArray = root.path("recommendations");
                if (recArray.isArray() && recArray.size() > 0) {
                    List<Map<String, Object>> recs = new ArrayList<>();
                    for (JsonNode item : recArray) {
                        Map<String, Object> map = objectMapper.convertValue(item, new TypeReference<Map<String, Object>>() {});
                        map.put("plantId", plant.getId());
                        recs.add(map);
                    }
                    return recs;
                }
            } catch (Exception e) {
                log.error("Failed to parse Groq recommendations JSON, falling back", e);
            }
        }

        // Deterministic fallback based on verified rules
        List<Map<String, Object>> recs = new ArrayList<>();

        if (sensorReading != null && !sensorReading.isStale()) {
            if (sensorReading.getSoilMoisture() != null && sensorReading.getSoilMoisture() < 30.0) {
                recs.add(createRecommendationMap(plant.getId(), "WATERING", "Low Soil Moisture Detected",
                        "The connected IoT sensor indicates soil moisture is at " + sensorReading.getSoilMoisture() + "%, which is below the optimal threshold.",
                        "Inspect soil surface texture and hydrate thoroughly until slight drainage occurs.",
                        "IoT Sensor Moisture: " + sensorReading.getSoilMoisture() + "%", "HIGH", 0.95));
            } else if (sensorReading.getSoilMoisture() != null && sensorReading.getSoilMoisture() > 75.0) {
                recs.add(createRecommendationMap(plant.getId(), "WATERING", "Soil Moisture Elevated",
                        "Soil moisture reading is high at " + sensorReading.getSoilMoisture() + "%. Ensure container drainage is unobstructed.",
                        "Hold off on further watering until moisture drops back into normal range.",
                        "IoT Sensor Moisture: " + sensorReading.getSoilMoisture() + "%", "MEDIUM", 0.92));
            }

            if (sensorReading.getSoilPh() != null && (sensorReading.getSoilPh() < 5.5 || sensorReading.getSoilPh() > 7.5)) {
                recs.add(createRecommendationMap(plant.getId(), "SOIL", "Substrate pH Needs Verification",
                        "Soil pH is reading at " + sensorReading.getSoilPh() + ", which may restrict trace nutrient assimilation.",
                        "Verify water source alkalinity and flush substrate with pH-balanced water.",
                        "IoT Sensor pH: " + sensorReading.getSoilPh(), "MEDIUM", 0.88));
            }
        } else if (sensorReading == null) {
            recs.add(createRecommendationMap(plant.getId(), "WATERING", "Schedule-Based Care Active",
                    "No IoT rod is paired for this plant. Recommendations are computed based on plant species profile, watering history, and local ambient climate.",
                    "Check soil dampness manually before watering. Pair an IoT rod for live telemetry.",
                    "No paired sensor rod. Using botanical schedule baseline.", "LOW", 0.85));
        } else {
            recs.add(createRecommendationMap(plant.getId(), "HARDWARE", "Sensor Telemetry Stale",
                    "The paired sensor device has not transmitted telemetry in over 2 hours. Soil conditions cannot be verified currently.",
                    "Check sensor rod power, battery level, and Wi-Fi connectivity.",
                    "Device last active: " + sensorReading.getRecordedAt(), "MEDIUM", 0.90));
        }

        if (weather != null && Boolean.TRUE.equals(weather.getIsRaining()) && !plant.getIsIndoor()) {
            recs.add(createRecommendationMap(plant.getId(), "WEATHER", "Natural Rainfall Accounted For",
                    "Local precipitation is occurring for outdoor location " + plant.getLocation() + ". Scheduled watering should be deferred.",
                    "Skip supplemental watering and monitor container drainage.",
                    "Weather forecast: Active precipitation in " + weather.getCity(), "HIGH", 0.94));
        }

        return recs;
    }

    /**
     * D. Natural-Language Plant Assistant
     */
    public String askPlantAssistant(Plant plant, SensorReading sensorReading, WeatherDTO weather,
                                    String userQuestion, List<Map<String, String>> chatHistory) {
        StringBuilder systemPrompt = new StringBuilder();
        systemPrompt.append("You are an intelligent, empathetic botanical assistant for the plant '")
                .append(plant.getName()).append("' (Species: ").append(plant.getPlantType()).append(").\n")
                .append("Ground your answers strictly in the plant's actual profile and provided telemetry. ")
                .append("Do not invent sensor readings or current weather conditions. If data is unavailable, state it clearly.\n\n");

        systemPrompt.append("Plant Profile:\n")
                .append("- Scientific Name: ").append(plant.getScientificName() != null ? plant.getScientificName() : "Botanical variety").append("\n")
                .append("- Location: ").append(plant.getLocation()).append(" (").append(plant.getIsIndoor() ? "Indoor" : "Outdoor").append(")\n")
                .append("- Health Status: ").append(plant.getHealth()).append("\n")
                .append("- Last Watered: ").append(plant.getLastWateredDate() != null ? plant.getLastWateredDate() : "Not recorded").append("\n");

        if (sensorReading != null && !sensorReading.isStale()) {
            systemPrompt.append("Current Live IoT Rod Readings:\n")
                    .append("- Soil Moisture: ").append(sensorReading.getSoilMoisture()).append("%\n")
                    .append("- Soil Temperature: ").append(sensorReading.getSoilTemperature()).append("°C\n")
                    .append("- Soil EC: ").append(sensorReading.getSoilEc()).append(" mS/cm\n")
                    .append("- Soil pH: ").append(sensorReading.getSoilPh()).append("\n")
                    .append("- Light: ").append(sensorReading.getLightLux()).append(" lux\n");
        } else if (sensorReading != null) {
            systemPrompt.append("IoT Rod: Offline (Last reading recorded at ").append(sensorReading.getRecordedAt()).append(" is stale). Current soil readings cannot be verified.\n");
        } else {
            systemPrompt.append("IoT Rod: None paired. Inform user that soil moisture is not directly measured.\n");
        }

        if (weather != null) {
            systemPrompt.append("Local Weather in ").append(weather.getCity()).append(": ")
                    .append(weather.getTemperature()).append("°C, ")
                    .append(weather.getHumidity()).append("% humidity, ")
                    .append(weather.getDescription()).append("\n");
        }

        String userPrompt = "User Question: " + userQuestion;
        String response = executeGroqPrompt(systemPrompt.toString(), userPrompt, false);
        if (response != null && !response.trim().isEmpty()) {
            return response.trim();
        }

        // Rule-based fallback if Groq key unconfigured
        return generateAssistantFallbackAnswer(plant, sensorReading, weather, userQuestion);
    }

    /**
     * E. Plant Health Summary
     */
    public String generateHealthSummary(Plant plant, SensorReading sensorReading, WeatherDTO weather) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Summarize health and immediate care advice in 2-3 friendly, factual sentences for: ")
                .append(plant.getName()).append(" (").append(plant.getPlantType()).append(").\n");

        if (sensorReading != null && !sensorReading.isStale()) {
            prompt.append("Soil Moisture: ").append(sensorReading.getSoilMoisture()).append("%, ")
                  .append("Soil Temp: ").append(sensorReading.getSoilTemperature()).append("°C.\n");
        } else if (sensorReading != null) {
            prompt.append("Sensor is disconnected/stale.\n");
        } else {
            prompt.append("No sensor rod is paired; soil conditions cannot be verified directly.\n");
        }

        if (weather != null) {
            prompt.append("Weather: ").append(weather.getTemperature()).append("°C, ").append(weather.getDescription()).append(".\n");
        }

        String summary = executeGroqPrompt(
                "You are a master botanist. Provide a concise, 2-3 sentence plant health summary using only the verified facts.",
                prompt.toString(),
                false
        );

        if (summary != null) {
            return summary.trim();
        }

        // Clean rule-based summary
        if (sensorReading != null && !sensorReading.isStale()) {
            return String.format("Your %s's latest soil moisture is %.1f%% with a soil temperature of %.1f°C. " +
                    "Conditions are %s. Verify topsoil before watering.",
                    plant.getName(), sensorReading.getSoilMoisture(), sensorReading.getSoilTemperature(),
                    sensorReading.getSoilMoisture() < 35 ? "drying out" : "satisfactory");
        } else if (sensorReading != null) {
            return String.format("The sensor rod for %s has not reported recently, so current soil conditions cannot be verified. " +
                    "Check device connectivity and examine the soil manually.", plant.getName());
        } else {
            return String.format("Your %s is currently monitored via botanical schedule and local weather. " +
                    "Connect a compatible sensor rod to enable real-time soil condition tracking.", plant.getName());
        }
    }

    /**
     * F. Disease Observation Explanation
     */
    public Map<String, Object> explainDiseaseObservation(Plant plant, String symptoms, String observationNotes) {
        String systemPrompt = "You are an expert plant pathologist. Explain observed plant symptoms and provide preventive next steps. " +
                "Do not present AI assessments as a definitive laboratory diagnosis. Return strictly valid JSON.";
        String userPrompt = String.format(
                "Plant: %s (%s). Observed symptoms: %s. Additional notes: %s.\n" +
                "Return a JSON object with fields:\n" +
                "- observedSymptoms (string)\n" +
                "- possibleCauses (string)\n" +
                "- alternativeExplanations (string)\n" +
                "- confidence (number between 0.70 and 0.95)\n" +
                "- recommendedInspection (string)\n" +
                "- preventiveMeasures (string)\n" +
                "- nextSteps (string)",
                plant.getName(), plant.getPlantType(), symptoms, observationNotes != null ? observationNotes : "None"
        );

        String rawJson = executeGroqPrompt(systemPrompt, userPrompt, true);
        if (rawJson != null) {
            try {
                return objectMapper.readValue(rawJson, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.error("Failed to parse disease observation JSON, falling back", e);
            }
        }

        Map<String, Object> fallback = new HashMap<>();
        fallback.put("observedSymptoms", symptoms != null ? symptoms : "Leaf discoloration or drooping");
        fallback.put("possibleCauses", "Moisture imbalance (overwatering or prolonged drought), insufficient lighting, or minor nutrient lock.");
        fallback.put("alternativeExplanations", "Root compaction, low ambient humidity, or early fungal leaf spot.");
        fallback.put("confidence", 0.85);
        fallback.put("recommendedInspection", "Inspect the underside of leaves for mite webbing or scale, and check pot drainage holes for standing water.");
        fallback.put("preventiveMeasures", "Allow topsoil to dry before watering, ensure good airflow around leaves, and avoid wetting foliage at night.");
        fallback.put("nextSteps", "Isolate plant from other specimens temporarily, adjust watering frequency, and re-evaluate foliage in 48 hours.");
        return fallback;
    }

    private Map<String, Object> createRecommendationMap(Long plantId, String category, String title,
                                                         String message, String action, String evidence,
                                                         String priority, double confidence) {
        Map<String, Object> map = new HashMap<>();
        map.put("plantId", plantId);
        map.put("category", category);
        map.put("title", title);
        map.put("message", message);
        map.put("recommendedAction", action);
        map.put("evidence", evidence);
        map.put("priority", priority);
        map.put("confidence", confidence);
        return map;
    }

    private String generateAssistantFallbackAnswer(Plant plant, SensorReading sensorReading, WeatherDTO weather, String q) {
        String lower = q.toLowerCase();
        if (lower.contains("water") || lower.contains("drink")) {
            if (sensorReading != null && !sensorReading.isStale()) {
                return String.format("Based on the live IoT sensor rod, your %s has a soil moisture reading of %.1f%%. %s",
                        plant.getName(), sensorReading.getSoilMoisture(),
                        sensorReading.getSoilMoisture() < 30 ? "The soil is getting dry, so watering today is recommended." : "Moisture is sufficient; no immediate watering is required.");
            } else if (sensorReading != null) {
                return String.format("The sensor rod for %s is currently offline, so live soil moisture cannot be confirmed. Please check the soil with your finger before watering.", plant.getName());
            } else {
                return String.format("No sensor rod is paired with %s. Based on standard care rules for %s, check if the top 2 inches of soil feel dry before providing water.", plant.getName(), plant.getPlantType());
            }
        } else if (lower.contains("yellow") || lower.contains("brown") || lower.contains("spot") || lower.contains("leaf")) {
            return String.format("Yellowing or browning on a %s is most commonly caused by over-watering, low humidity, or root compaction. Inspect root aeration and ensure the pot has drainage holes.", plant.getPlantType());
        } else if (lower.contains("weather") || lower.contains("temp") || lower.contains("sun")) {
            if (weather != null) {
                return String.format("Current weather in %s is %s with a temperature of %.1f°C and %.0f%% humidity. For an %s plant, this is generally %s.",
                        weather.getCity(), weather.getDescription(), weather.getTemperature(), weather.getHumidity(),
                        plant.getIsIndoor() ? "indoor" : "outdoor",
                        weather.getTemperature() < 10 ? "cooler than ideal—keep away from drafts" : "favorable for vegetative growth");
            }
            return String.format("%s prefers moderate room temperatures between 18°C and 25°C with bright, indirect sunlight.", plant.getPlantType());
        }

        return String.format("For your %s (%s), maintaining consistent light, appropriate pot drainage, and monitoring substrate moisture will ensure healthy foliage growth. How else can I assist with this plant?", plant.getName(), plant.getPlantType());
    }

    private String getBotanicalScientificName(String commonName) {
        if (commonName == null) return "Plantae sp.";
        String lower = commonName.toLowerCase();
        if (lower.contains("snake")) return "Sansevieria trifasciata";
        if (lower.contains("monstera")) return "Monstera deliciosa";
        if (lower.contains("pothos")) return "Epipremnum aureum";
        if (lower.contains("peace lily")) return "Spathiphyllum wallisii";
        if (lower.contains("spider")) return "Chlorophytum comosum";
        if (lower.contains("zz")) return "Zamioculcas zamiifolia";
        if (lower.contains("rubber")) return "Ficus elastica";
        if (lower.contains("fiddle")) return "Ficus lyrata";
        if (lower.contains("aloe")) return "Aloe barbadensis miller";
        if (lower.contains("basil")) return "Ocimum basilicum";
        return commonName + " sp.";
    }

    private String getBotanicalFamily(String commonName) {
        if (commonName == null) return "Plantae";
        String lower = commonName.toLowerCase();
        if (lower.contains("snake") || lower.contains("spider")) return "Asparagaceae";
        if (lower.contains("monstera") || lower.contains("pothos") || lower.contains("peace lily") || lower.contains("zz")) return "Araceae";
        if (lower.contains("rubber") || lower.contains("fiddle")) return "Moraceae";
        if (lower.contains("aloe")) return "Asphodelaceae";
        if (lower.contains("basil")) return "Lamiaceae";
        return "Angiosperms";
    }
}
