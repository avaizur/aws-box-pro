package com.aidocanalysis.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.Map;

/**
 * AiServiceClient — HTTP client for the Python Flask AI service.
 *
 * The Java API is the orchestrator. It calls this client to delegate
 * all text analysis work to the Python service.
 *
 * Python AI service contract:
 *   POST http://localhost:5000/analyze
 *   Request:  { "text": "..." }
 *   Response: { "summary": "...", "word_count": 42, "classification": "technical", "processing_ms": 12 }
 */
@Service
public class AiServiceClient {

    private final RestTemplate restTemplate;
    private final String aiServiceUrl;

    public AiServiceClient(@Value("${ai.service.url}") String aiServiceUrl) {
        this.restTemplate = new RestTemplate();
        this.aiServiceUrl = aiServiceUrl;
    }

    /**
     * Send text to the Python AI service and return the analysis result as a Map.
     *
     * @param text   The raw document text to analyse.
     * @param engine The engine to use: "local" (default) or "bedrock".
     * @return Map containing: summary, word_count, classification, processing_ms, engine
     */
    public Map<String, Object> analyze(String text, String engine) {
        String url = aiServiceUrl + "/analyze";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Include the engine choice in the request body
        Map<String, String> body = Map.of(
            "text", text,
            "engine", engine != null ? engine : "local"
        );
        
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map<String, Object>> response =
                restTemplate.exchange(url, HttpMethod.POST, request,
                        new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("AI service returned an error: " + response.getStatusCode());
        }

        return response.getBody();
    }
}
