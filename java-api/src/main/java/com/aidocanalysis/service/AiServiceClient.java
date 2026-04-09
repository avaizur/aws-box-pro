package com.aidocanalysis.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.core.SdkBytes;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * AiServiceClient — Client for document analysis.
 * Supports both local Flask service (HTTP) and AWS Lambda (Serverless).
 */
@Service
public class AiServiceClient {

    private final RestTemplate restTemplate;
    private final String aiServiceUrl;
    private final String aiServiceType;
    private final String lambdaFunctionName;
    private final LambdaClient lambdaClient;
    private final ObjectMapper objectMapper;

    public AiServiceClient(
            @Value("${ai.service.url}") String aiServiceUrl,
            @Value("${ai.service.type:http}") String aiServiceType,
            @Value("${ai.service.lambda.name:ai-doc-analysis-analyzer}") String lambdaFunctionName) {
        this.restTemplate = new RestTemplate();
        this.aiServiceUrl = aiServiceUrl;
        this.aiServiceType = aiServiceType;
        this.lambdaFunctionName = lambdaFunctionName;
        this.objectMapper = new ObjectMapper();
        
        if ("lambda".equalsIgnoreCase(aiServiceType)) {
            this.lambdaClient = LambdaClient.builder().build();
        } else {
            this.lambdaClient = null;
        }
    }

    public Map<String, Object> analyze(String text, String engine) {
        if ("lambda".equalsIgnoreCase(aiServiceType)) {
            return analyzeWithLambda(text, engine);
        } else {
            return analyzeWithHttp(text, engine);
        }
    }

    private Map<String, Object> analyzeWithHttp(String text, String engine) {
        String url = aiServiceUrl + "/analyze";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

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

    private Map<String, Object> analyzeWithLambda(String text, String engine) {
        try {
            Map<String, String> payloadMap = Map.of(
                "text", text,
                "engine", engine != null ? engine : "local"
            );
            String payload = objectMapper.writeValueAsString(payloadMap);

            InvokeRequest request = InvokeRequest.builder()
                    .functionName(lambdaFunctionName)
                    .payload(SdkBytes.fromUtf8String(payload))
                    .build();

            InvokeResponse response = lambdaClient.invoke(request);
            String responseStr = response.payload().asUtf8String();
            
            // Lambda returns { "statusCode": 200, "body": "{...}" } if it's acting like a proxy,
            // or just the JSON if direct. Our lambda_function.py returns both.
            Map<String, Object> fullResponse = objectMapper.readValue(responseStr, Map.class);
            
            if (fullResponse.containsKey("body")) {
                return objectMapper.readValue((String) fullResponse.get("body"), Map.class);
            }
            return fullResponse;
        } catch (Exception e) {
            throw new RuntimeException("Lambda analysis failed: " + e.getMessage(), e);
        }
    }
}
