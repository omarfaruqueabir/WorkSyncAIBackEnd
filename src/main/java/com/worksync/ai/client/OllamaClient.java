package com.worksync.ai.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
public class OllamaClient {

    @Value("${ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${ollama.model:llama2}")
    private String defaultModel;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OllamaClient() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public String generateCompletion(String systemPrompt, String userPrompt) {
        return generateCompletionWithModel(defaultModel, systemPrompt, userPrompt);
    }

    public String generateCompletionWithModel(String model, String systemPrompt, String userPrompt) {
        try {
            String url = baseUrl + "/api/generate";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Combine system and user prompts
            String fullPrompt = String.format("System: %s\n\nUser: %s", systemPrompt, userPrompt);

            Map<String, Object> requestBody = Map.of(
                "model", model,
                "prompt", fullPrompt,
                "stream", false
            );

            log.debug("Request body for Ollama: {}", objectMapper.writeValueAsString(requestBody));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.debug("Sending request to Ollama API with model {}: {}", model, url);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String generatedResponse = (String) response.getBody().get("response");
                log.debug("Generated response: {}", generatedResponse);
                return generatedResponse;
            }

            log.error("Unexpected response from Ollama API: {} with body: {}", 
                response.getStatusCode(), response.getBody());
            return null;

        } catch (Exception e) {
            log.error("Error calling Ollama API with model {}: {}", model, e.getMessage(), e);
            return null;
        }
    }

    public float[] generateEmbedding(String text) {
        try {
            String url = baseUrl + "/api/embeddings";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = Map.of(
                "model", defaultModel,
                "prompt", text
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Object[] embedding = (Object[]) response.getBody().get("embedding");
                float[] result = new float[embedding.length];
                for (int i = 0; i < embedding.length; i++) {
                    result[i] = ((Number) embedding[i]).floatValue();
                }
                return result;
            }

            log.error("Unexpected response from Ollama embeddings API: {}", response.getStatusCode());
            return null;

        } catch (Exception e) {
            log.error("Error generating embedding: {}", e.getMessage(), e);
            return null;
        }
    }
} 