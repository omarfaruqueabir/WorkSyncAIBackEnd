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

            if (matches.isEmpty()) {
                return ChatbotResponse.builder()
                    .success(true)
                    .message(fallbackMessage)
                    .matches(List.of())
                    .build();
            }

            // Process the data based on query type
            String response = switch (analysis.getQueryType()) {
                case SIMPLE_RETRIEVAL -> handleSimpleRetrieval(matches, request.query());
                case ANALYTICAL, STATISTICAL -> handleAnalyticalQuery(matches, request.query(), analysis);
                case TEMPORAL -> handleTemporalQuery(matches, request.query(), analysis);
                case COMPARATIVE -> handleComparativeQuery(matches, request.query(), analysis);
                case AGGREGATIVE -> handleAggregativeQuery(matches, request.query(), analysis);
                default -> handleSimpleRetrieval(matches, request.query());
            };

            return ChatbotResponse.builder()
                .success(true)
                .message(response)
                .matches(List.of()) // We don't return matches since we're providing processed response
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

    private List<SummaryMatch> fetchRelevantData(String query, QueryAnalysis analysis) {
        // Get initial matches based on vector similarity
        List<SummaryMatch> matches = vectorStorageService.similaritySearch(
            query,
            10 // Default to 10 results
        );

        // Apply additional filters based on analysis
        if (analysis.getEmployeeId() != null) {
            matches = matches.stream()
                .filter(m -> m.employeeId().equals(analysis.getEmployeeId()))
                .collect(Collectors.toList());
        }

        // Apply keyword filters if specified
        if (analysis.getFilterKeywords() != null && !analysis.getFilterKeywords().isEmpty()) {
            matches = filterSummariesByKeywords(matches, analysis.getFilterKeywords());
        }

        return matches;
    }

    private String handleSimpleRetrieval(List<SummaryMatch> matches, String query) {
        // For simple retrieval, use the existing response generation approach
        return generateAIProcessedResponse(query, matches);
    }

    private String handleAnalyticalQuery(List<SummaryMatch> matches, String query, QueryAnalysis analysis) {
        String contextData = formatMatchesForAnalysis(matches);
        String metrics = String.join(", ", analysis.getRequiredFields());
        
        String prompt = String.format(
            ANALYTICAL_PROMPT_TEMPLATE,
            analysis.getQueryType(),
            metrics,
            analysis.getTimeframe() != null ? analysis.getTimeframe() : "all time",
            analysis.isRequiresAggregation() ? "required" : "not required",
            contextData,
            query
        );

        return openRouterClient.chatCompletionWithModel(
            "openai/gpt-4",
            "You are a data analysis expert. Provide detailed, specific answers based on the data.",
            prompt,
            0.3,
            1000
        );
    }

    private String handleTemporalQuery(List<SummaryMatch> matches, String query, QueryAnalysis analysis) {
        // Similar to analytical but with focus on temporal aspects
        return handleAnalyticalQuery(matches, query, analysis);
    }

    private String handleComparativeQuery(List<SummaryMatch> matches, String query, QueryAnalysis analysis) {
        // Similar to analytical but with focus on comparison
        return handleAnalyticalQuery(matches, query, analysis);
    }

    private String handleAggregativeQuery(List<SummaryMatch> matches, String query, QueryAnalysis analysis) {
        // Similar to analytical but with focus on aggregation
        return handleAnalyticalQuery(matches, query, analysis);
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

    private String generateAIProcessedResponse(String query, List<SummaryMatch> matches) {
        try {
            // Prepare context from matched summaries
            StringBuilder contextBuilder = new StringBuilder();
            contextBuilder.append("User Query: ").append(query).append("\n\n");
            contextBuilder.append("Available Summary Data:\n");

            for (int i = 0; i < Math.min(matches.size(), 5); i++) {
                SummaryMatch match = matches.get(i);
                contextBuilder.append("--- Summary ").append(i + 1).append(" (Employee: ")
                    .append(match.employeeId()).append(", Similarity: ")
                    .append(String.format("%.3f", match.similarity())).append(") ---\n");
                contextBuilder.append(match.summary()).append("\n\n");
            }

            String userPrompt = contextBuilder.toString() + 
                "\nINSTRUCTIONS:\n" +
                "1. Read the user's query carefully and identify what SPECIFIC information they want\n" +
                "2. Search through ALL the summary data for the exact details requested\n" +
                "3. Extract and present the SPECIFIC information (URLs, app names, timestamps, etc.)\n" +
                "4. Quote the relevant parts of the summaries that contain the requested information\n" +
                "5. If asking about security events, include URLs, threat types, and timestamps\n" +
                "6. If asking about applications, include app names, durations, and usage details\n" +
                "7. Be precise and factual - don't say information is missing if it's clearly present in the summaries\n" +
                "8. Format your response with clear headings and bullet points for specific details\n\n" +
                "Provide a direct, accurate answer based on the summary data above.";

            // Use GPT-4 for better accuracy in information extraction
            String aiResponse = openRouterClient.chatCompletionWithModel(
                "openai/gpt-4",  // Use GPT-4 instead of Llama
                "You are an expert AI assistant for employee monitoring and security analysis. " +
                "Your task is to extract and present SPECIFIC information from employee activity summaries. " +
                "When users ask about specific details (URLs, app names, timestamps, security incidents, etc.), " +
                "you MUST extract the EXACT information from the summaries provided. " +
                
                "CRITICAL RULES: " +
                "- NEVER refer to 'Summary 1', 'Summary 2', etc. Instead, provide the actual information directly " +
                "- For security-related queries: Extract and provide the EXACT URLs, domain names, IP addresses mentioned " +
                "- For application queries: Provide the EXACT application names, durations, and usage details " +
                "- For incident queries: Provide the EXACT timestamps, alert types, severity levels, and descriptions " +
                "- If a specific URL is mentioned in the content, include it verbatim in your response " +
                "- Always quote exact values, names, URLs, and timestamps from the source material " +
                
                "RESPONSE FORMAT: " +
                "- Start with the direct answer to the user's question " +
                "- Include all relevant specific details (URLs, times, names, etc.) " +
                "- Provide context about the incident or activity " +
                "- Use clear, professional language " +
                "- If information is not available, state that clearly " +
                
                "EXAMPLE: If asked about malware URLs, respond like: " +
                "'John accessed the malicious website suspicious-site.com at 2025-05-27T19:30:12. " +
                "This site was identified as a phishing threat with a risk score of 9.2/10 and was blocked by the security system.' " +
                
                "Remember: Extract EXACT details from the content, never invent information, and avoid generic summary references.",
                userPrompt,
                0.3,  // Lower temperature for more factual responses
                1000  // Allow more tokens for detailed responses
            );

            if (aiResponse != null && !aiResponse.trim().isEmpty()) {
                log.debug("Generated AI-processed response for query: {}", query);
                return aiResponse.trim();
            }

            return null;

        } catch (Exception e) {
            log.error("Error generating AI-processed response: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Helper method to filter summaries based on specific keywords or criteria
     * This can be used for additional filtering if needed
     */
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