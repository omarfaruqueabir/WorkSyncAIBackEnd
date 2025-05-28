package com.worksync.ai.service.impl;

import com.worksync.ai.client.OpenRouterClient;
import com.worksync.ai.model.dto.*;
import com.worksync.ai.model.enums.QueryType;
import com.worksync.ai.service.ChatbotService;
import com.worksync.ai.service.EmbeddingAndVectorStorageService;
import com.worksync.ai.service.QueryAnalyzerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatbotServiceImpl implements ChatbotService {

    @Autowired
    private EmbeddingAndVectorStorageService vectorStorageService;

    @Autowired
    private QueryAnalyzerService queryAnalyzerService;

    @Autowired
    private OpenRouterClient openRouterClient;

    @Value("${spring.ai.openai.chat.model:deepseek/deepseek-prover-v2:free}")
    private String model;

    @Value("${spring.ai.openai.chat.options.temperature:0.3}")
    private double temperature;

    @Value("${spring.ai.openai.chat.options.max-tokens:2000}")
    private int maxTokens;

    @Value("${chatbot.rag.fallback.similarity-threshold:0.2}")
    private double similarityThreshold;

    @Value("${chatbot.rag.fallback.message}")
    private String fallbackMessage;

    private static final String ANALYTICAL_PROMPT_TEMPLATE = """
        You are analyzing employee activity data. Process the following information and provide a detailed answer.
        
        Query Type: %s
        Analysis Requirements:
        - Metrics: %s
        - Time Frame: %s
        - Aggregation: %s
        
        Available Data:
        %s
        
        User Query: %s
        
        Instructions:
        1. Focus on extracting SPECIFIC metrics and values
        2. If aggregating data, show the calculations
        3. For temporal analysis, consider the time periods
        4. Include exact numbers, durations, and timestamps
        5. Format the response with clear sections
        6. If data is insufficient, state what's missing
        
        Provide a detailed, data-driven response.
        """;

    @Override
    public ChatbotResponse processQuery(ChatbotRequest request) {
        log.debug("Processing chatbot query: {}", request.query());
        
        try {
            // First, analyze the query to determine processing requirements
            QueryAnalysis analysis = queryAnalyzerService.analyzeQuery(request.query());
            log.debug("Query analysis result: {}", analysis);

            // Fetch relevant data based on analysis
            List<SummaryMatch> matches = fetchRelevantData(request.query(), analysis);
            log.debug("Found {} relevant matches", matches.size());

            if (matches.isEmpty()) {
                return handleNoMatches(request.query(), analysis);
            }

            // Process the data based on query type
            String response = processQueryByType(matches, request.query(), analysis);
            
            if (response == null || response.trim().isEmpty()) {
                log.warn("Generated response was empty, using fallback");
                return handleNoMatches(request.query(), analysis);
            }

            return ChatbotResponse.builder()
                .success(true)
                .message(response)
                .matches(matches)
                .build();

        } catch (Exception e) {
            log.error("Error processing chatbot query: {}", e.getMessage(), e);
            return ChatbotResponse.builder()
                .success(false)
                .message("Error processing your query: " + e.getMessage())
                .matches(List.of())
                .build();
        }
    }

    private String processQueryByType(List<SummaryMatch> matches, String query, QueryAnalysis analysis) {
        String contextData = formatMatchesForAnalysis(matches);
        String systemPrompt = getSystemPromptForQueryType(analysis.getQueryType());
        
        String userPrompt = buildDetailedPrompt(matches, query, analysis);
        
        try {
            String response = openRouterClient.chatCompletionWithModel(
                model,
                systemPrompt,
                userPrompt,
                temperature,
                maxTokens
            );
            
            if (response != null && !response.trim().isEmpty()) {
                log.debug("Generated response for query type {}: {} chars", 
                    analysis.getQueryType(), response.length());
                return response.trim();
            }
            
            log.warn("Empty response from OpenRouter for query type: {}", analysis.getQueryType());
            return null;
            
        } catch (Exception e) {
            log.error("Error generating response: {}", e.getMessage(), e);
            return null;
        }
    }

    private String getSystemPromptForQueryType(QueryType queryType) {
        return switch (queryType) {
            case ANALYTICAL, STATISTICAL -> 
                "You are a data analysis expert specializing in employee activity data. " +
                "Present your analysis in a clear, structured format with sections for Summary, Key Details, " +
                "Specifics, Metrics, and Timeline. Focus on extracting and organizing specific data points " +
                "in a way that's easy to read and understand.";
            case TEMPORAL -> 
                "You are a temporal data analysis expert. Organize your findings chronologically " +
                "with clear sections for Summary, Key Details, Specifics, Metrics, and Timeline. " +
                "Emphasize time-based patterns and present events in a clear sequence.";
            case COMPARATIVE -> 
                "You are a comparative analysis expert. Structure your comparison with sections " +
                "for Summary, Key Details, Specifics, Metrics, and Timeline. Present differences " +
                "and similarities in a clear, parallel format.";
            case AGGREGATIVE -> 
                "You are a data aggregation expert. Present aggregated data in organized sections " +
                "for Summary, Key Details, Specifics, Metrics, and Timeline. Focus on clear presentation " +
                "of combined metrics and trends.";
            default -> 
                "You are an expert AI assistant for employee monitoring and security analysis. " +
                "Present information in clear sections for Summary, Key Details, Specifics, Metrics, " +
                "and Timeline. Focus on extracting and organizing specific information in a structured format.";
        };
    }

    private String buildDetailedPrompt(List<SummaryMatch> matches, String query, QueryAnalysis analysis) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("User Query: ").append(query).append("\n\n");

        // Add relevant context if available
        if (analysis.getTimeframe() != null || analysis.getRequiredFields() != null || analysis.isRequiresAggregation()) {
            prompt.append("Context:\n");
            if (analysis.getTimeframe() != null) {
                prompt.append("- Time Period: ").append(analysis.getTimeframe()).append("\n");
            }
            if (analysis.getRequiredFields() != null && !analysis.getRequiredFields().isEmpty()) {
                prompt.append("- Required Info: ").append(String.join(", ", analysis.getRequiredFields())).append("\n");
            }
            if (analysis.isRequiresAggregation()) {
                prompt.append("- Needs Aggregation: Yes\n");
            }
            prompt.append("\n");
        }
        
        // Add available data
        prompt.append("Available Data:\n");
        matches.forEach(match -> {
            prompt.append("---\n");
            prompt.append("Employee: ").append(match.employeeId()).append("\n");
            prompt.append("Data: ").append(match.summary()).append("\n");
        });

        // Response formatting instructions
        prompt.append("""

            Important Instructions:
            1. ONLY answer what was specifically asked in the query
            2. Do not include additional information unless explicitly requested
            3. Keep the response focused and concise
            4. For ID/name queries, only return the specific ID or name
            5. For usage queries, only return the specific usage asked about
            6. Skip sections that aren't relevant to the specific query

            Format your response using these sections only if relevant to the query:
            ### SUMMARY
            One clear sentence answering the query

            ### DETAILS
            Only if additional details were specifically requested

            ### METRICS
            Only if specific numbers/stats were requested

            ### TIMELINE
            Only if time-based information was requested
            """);
        
        return prompt.toString();
    }

    private List<SummaryMatch> fetchRelevantData(String query, QueryAnalysis analysis) {
        // Get initial matches based on vector similarity
        List<SummaryMatch> matches = vectorStorageService.similaritySearch(
            query,
            10 // Default to 10 results
        );

        log.debug("Initial vector search returned {} matches", matches.size());

        // Apply additional filters based on analysis
        if (analysis.getEmployeeId() != null) {
            matches = matches.stream()
                .filter(m -> m.employeeId().equals(analysis.getEmployeeId()))
                .collect(Collectors.toList());
            log.debug("After employee filter: {} matches", matches.size());
        }

        // Apply keyword filters if specified
        if (analysis.getFilterKeywords() != null && !analysis.getFilterKeywords().isEmpty()) {
            matches = filterSummariesByKeywords(matches, analysis.getFilterKeywords());
            log.debug("After keyword filter: {} matches", matches.size());
        }

        // Filter out low similarity matches
        matches = matches.stream()
            .filter(m -> m.similarity() >= similarityThreshold)
            .collect(Collectors.toList());
        log.debug("After similarity threshold filter: {} matches", matches.size());

        return matches;
    }

    private String formatMatchesForAnalysis(List<SummaryMatch> matches) {
        StringBuilder builder = new StringBuilder();
        for (SummaryMatch match : matches) {
            builder.append("---\n");
            builder.append("Employee: ").append(match.employeeId()).append("\n");
            builder.append("Timestamp: ").append(match.timestamp()).append("\n");
            builder.append("Data: ").append(match.summary()).append("\n");
        }
        return builder.toString();
    }

    private List<SummaryMatch> filterSummariesByKeywords(List<SummaryMatch> matches, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return matches;
        }

        return matches.stream()
            .filter(match -> {
                String summaryText = match.summary().toLowerCase();
                return keywords.stream().anyMatch(keyword -> 
                    summaryText.contains(keyword.toLowerCase()));
            })
            .collect(Collectors.toList());
    }

    private ChatbotResponse handleNoMatches(String query, QueryAnalysis analysis) {
        try {
            String systemPrompt = """
                You are an AI assistant helping with employee activity queries.
                When no matching data is found, provide a clear and specific response about what was not found.
                Focus on the specific elements from the query (employee, application, activity type, etc.).
                """;

            String userPrompt = String.format("""
                Query: %s
                
                Analysis:
                - Employee ID: %s
                - Employee Name: %s
                - Required Fields: %s
                - Time Period: %s
                
                No matching data was found in the system.
                Explain specifically what information was looked for but not found.
                Format the response in a clear, direct way using the ### SUMMARY format.
                """,
                query,
                analysis.getEmployeeId(),
                analysis.getEmployeeName(),
                analysis.getRequiredFields() != null ? String.join(", ", analysis.getRequiredFields()) : "none",
                analysis.getTimeframe() != null ? analysis.getTimeframe() : "not specified"
            );

            String response = openRouterClient.chatCompletionWithModel(
                model,
                systemPrompt,
                userPrompt,
                temperature,
                maxTokens
            );

            if (response != null && !response.trim().isEmpty()) {
                return ChatbotResponse.builder()
                    .success(true)
                    .message(response.trim())
                    .matches(List.of())
                    .build();
            }
        } catch (Exception e) {
            log.error("Error generating no-match response: {}", e.getMessage(), e);
        }

        // Ultimate fallback if everything fails
        return ChatbotResponse.builder()
            .success(true)
            .message(fallbackMessage)
            .matches(List.of())
            .build();
    }
} 