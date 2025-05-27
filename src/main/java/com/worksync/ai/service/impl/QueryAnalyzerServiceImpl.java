package com.worksync.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.worksync.ai.client.OpenRouterClient;
import com.worksync.ai.model.dto.QueryAnalysis;
import com.worksync.ai.model.enums.QueryType;
import com.worksync.ai.service.QueryAnalyzerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class QueryAnalyzerServiceImpl implements QueryAnalyzerService {

    @Autowired
    private OpenRouterClient openRouterClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${spring.ai.openai.chat.model:openai/gpt-4}")
    private String model;

    @Value("${spring.ai.openai.chat.options.temperature:0.3}")
    private double temperature;

    @Value("${spring.ai.openai.chat.options.max-tokens:1000}")
    private int maxTokens;

    private static final String QUERY_ANALYSIS_PROMPT = """
        Analyze the following user query and classify it according to these criteria:
        1. Query Type:
           - SIMPLE_RETRIEVAL: Direct information lookup
           - ANALYTICAL: Requires analysis (e.g., "most used", "least frequent")
           - TEMPORAL: Time-based analysis
           - COMPARATIVE: Comparing different aspects
           - AGGREGATIVE: Requires data aggregation
           - STATISTICAL: Statistical analysis
        
        2. Required Processing:
           - Identify if aggregation is needed
           - Extract time-related information
           - Identify specific metrics or measurements needed
           - Determine if employee-specific filtering is needed
        
        Respond with a JSON object containing:
        {
            "queryType": "QUERY_TYPE",
            "filterKeywords": ["keyword1", "keyword2"],
            "requiresAggregation": boolean,
            "timeframe": "specific timeframe or null",
            "employeeId": "employee identifier if specified",
            "requiredFields": ["field1", "field2"],
            "extractionCriteria": {
                "metric": "usage_duration",
                "aggregation": "sum",
                "orderBy": "duration",
                "orderDirection": "desc"
            }
        }
        
        Query to analyze: %s
        """;

    @Override
    public QueryAnalysis analyzeQuery(String query) {
        try {
            String analysisPrompt = String.format(QUERY_ANALYSIS_PROMPT, query);
            String analysisResponse = openRouterClient.chatCompletionWithModel(
                model,
                "You are a query analysis expert. Provide only the JSON response, no additional text.",
                analysisPrompt,
                temperature,
                maxTokens
            );

            if (analysisResponse == null || analysisResponse.trim().isEmpty()) {
                log.warn("Received empty analysis response for query: {}", query);
                return getDefaultAnalysis(query);
            }

            try {
                // Extract JSON from the response
                String jsonResponse = extractJsonFromResponse(analysisResponse);
                
                // Parse the JSON response
                Map<String, Object> analysisMap = objectMapper.readValue(jsonResponse, Map.class);

                return QueryAnalysis.builder()
                    .queryType(QueryType.valueOf((String) analysisMap.get("queryType")))
                    .filterKeywords((List<String>) analysisMap.get("filterKeywords"))
                    .requiresAggregation((Boolean) analysisMap.get("requiresAggregation"))
                    .timeframe((String) analysisMap.get("timeframe"))
                    .employeeId((String) analysisMap.get("employeeId"))
                    .requiredFields((List<String>) analysisMap.get("requiredFields"))
                    .extractionCriteria((Map<String, Object>) analysisMap.get("extractionCriteria"))
                    .build();

            } catch (Exception e) {
                log.error("Error parsing analysis response: {}", e.getMessage());
                log.debug("Raw response: {}", analysisResponse);
                return getDefaultAnalysis(query);
            }

        } catch (Exception e) {
            log.error("Error analyzing query: {}", e.getMessage(), e);
            return getDefaultAnalysis(query);
        }
    }

    private String extractJsonFromResponse(String response) {
        // Find the first '{' and last '}'
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        
        if (start >= 0 && end >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        
        throw new IllegalArgumentException("No valid JSON found in response");
    }

    private QueryAnalysis getDefaultAnalysis(String query) {
        return QueryAnalysis.builder()
            .queryType(QueryType.SIMPLE_RETRIEVAL)
            .filterKeywords(Arrays.asList(query.toLowerCase().split("\\s+")))
            .requiresAggregation(false)
            .build();
    }
} 