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
                return ChatbotResponse.builder()
                    .success(true)
                    .message(fallbackMessage)
                    .matches(List.of())
                    .build();
            }

            // Process the data based on query type
            String response = processQueryByType(matches, request.query(), analysis);
            
            if (response == null || response.trim().isEmpty()) {
                log.warn("Generated response was empty, using fallback");
                return ChatbotResponse.builder()
                    .success(true)
                    .message(fallbackMessage)
                    .matches(List.of())
                    .build();
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
        prompt.append("Context and Requirements:\n");
        
        if (analysis.getTimeframe() != null) {
            prompt.append("- Time Period: ").append(analysis.getTimeframe()).append("\n");
        }
        if (analysis.getRequiredFields() != null && !analysis.getRequiredFields().isEmpty()) {
            prompt.append("- Required Information: ").append(String.join(", ", analysis.getRequiredFields())).append("\n");
        }
        if (analysis.isRequiresAggregation()) {
            prompt.append("- Aggregation Required: Yes\n");
        }
        
        prompt.append("\nAvailable Data:\n");
        matches.forEach(match -> {
            prompt.append("---\n");
            prompt.append("Employee: ").append(match.employeeId()).append("\n");
            prompt.append("Relevance Score: ").append(String.format("%.2f", match.similarity())).append("\n");
            prompt.append("Data: ").append(match.summary()).append("\n\n");
        });

        prompt.append("\nResponse Format Instructions:\n");
        prompt.append("1. Structure your response in the following sections:\n");
        prompt.append("   a. SUMMARY: A 2-3 sentence overview of the findings\n");
        prompt.append("   b. KEY_DETAILS: Bullet points of the most important information\n");
        prompt.append("   c. SPECIFICS: Detailed information organized by category\n");
        prompt.append("   d. METRICS: Any relevant numbers, durations, or statistics\n");
        prompt.append("   e. TIMELINE: Time-based information if relevant\n\n");
        
        prompt.append("2. Format Rules:\n");
        prompt.append("   - Use ### to denote section headers\n");
        prompt.append("   - Use • for bullet points\n");
        prompt.append("   - Use --- for separating different items in the same category\n");
        prompt.append("   - Present numbers and metrics in a clear, readable format\n");
        prompt.append("   - Keep sentences concise and direct\n\n");
        
        prompt.append("3. Example Format:\n");
        prompt.append("### SUMMARY\n");
        prompt.append("<2-3 clear sentences>\n\n");
        prompt.append("### KEY_DETAILS\n");
        prompt.append("• Key point 1\n");
        prompt.append("• Key point 2\n\n");
        prompt.append("### SPECIFICS\n");
        prompt.append("Category 1:\n");
        prompt.append("• Detail 1\n");
        prompt.append("• Detail 2\n");
        prompt.append("---\n");
        prompt.append("Category 2:\n");
        prompt.append("• Detail 3\n");
        prompt.append("• Detail 4\n\n");
        prompt.append("### METRICS\n");
        prompt.append("• Metric 1: value\n");
        prompt.append("• Metric 2: value\n\n");
        prompt.append("### TIMELINE\n");
        prompt.append("• Time 1: event\n");
        prompt.append("• Time 2: event\n\n");
        
        prompt.append("4. Content Guidelines:\n");
        prompt.append("   - Extract and present SPECIFIC information from the data\n");
        prompt.append("   - Include exact details (timestamps, durations, app names, URLs)\n");
        prompt.append("   - For security events, include threat types and URLs\n");
        prompt.append("   - For application usage, include durations and categories\n");
        prompt.append("   - Present metrics in a clear, comparable format\n");
        prompt.append("   - If certain sections are not relevant, omit them\n");
        
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
} 