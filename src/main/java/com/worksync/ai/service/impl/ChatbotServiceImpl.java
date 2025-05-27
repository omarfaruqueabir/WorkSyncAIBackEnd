package com.worksync.ai.service.impl;

import com.worksync.ai.client.OpenRouterClient;
import com.worksync.ai.model.dto.ChatbotRequest;
import com.worksync.ai.model.dto.ChatbotResponse;
import com.worksync.ai.model.dto.SummaryMatch;
import com.worksync.ai.service.ChatbotService;
import com.worksync.ai.service.EmbeddingAndVectorStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatbotServiceImpl implements ChatbotService {

    @Autowired
    private EmbeddingAndVectorStorageService vectorStorageService;

    @Autowired
    private OpenRouterClient openRouterClient;

    @Value("${chatbot.rag.fallback.similarity-threshold:0.2}")
    private double similarityThreshold;

    @Value("${chatbot.rag.fallback.message}")
    private String fallbackMessage;

    private static final String INTENT_ANALYSIS_SYSTEM_PROMPT = 
        "You are an AI assistant that analyzes user queries to extract specific intent and filter criteria. " +
        "Respond with a JSON object containing: " +
        "{ \"intent\": \"brief description of what user wants\", " +
        "\"filter_keywords\": [\"keyword1\", \"keyword2\"], " +
        "\"requires_specific_data\": true/false, " +
        "\"specific_type\": \"performance_alerts\" | \"security_events\" | \"app_usage\" | \"general\" }";

    private static final String RESPONSE_GENERATION_SYSTEM_PROMPT = 
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
        
        "Remember: Extract EXACT details from the content, never invent information, and avoid generic summary references.";

    @Override
    public ChatbotResponse processQuery(ChatbotRequest request) {
        log.debug("Processing chatbot query: {}", request.query());
        
        try {
            // First, perform similarity search to get relevant summaries
            List<SummaryMatch> matches = vectorStorageService.similaritySearch(
                request.query(),
                request.topK() != null ? request.topK() : 10
            );

            // If no relevant matches found, return fallback message
            if (matches.isEmpty() || matches.get(0).similarity() < similarityThreshold) {
                log.debug("No relevant matches found for query: {}", request.query());
                return ChatbotResponse.builder()
                    .success(true)
                    .message(fallbackMessage)
                    .matches(List.of())
                    .build();
            }

            log.debug("Found {} relevant matches for query", matches.size());

            // Use AI to analyze user intent and generate focused response
            String processedResponse = generateAIProcessedResponse(request.query(), matches);

            if (processedResponse != null && !processedResponse.trim().isEmpty()) {
                return ChatbotResponse.builder()
                    .success(true)
                    .message(processedResponse)
                    .matches(List.of()) // Return empty matches since AI has processed the data
                    .build();
            } else {
                // Fallback to original behavior if AI processing fails
                return ChatbotResponse.builder()
                    .success(true)
                    .message("Found relevant information")
                    .matches(matches)
                    .build();
            }

        } catch (Exception e) {
            log.error("Error processing chatbot query: {}", e.getMessage(), e);
            return ChatbotResponse.builder()
                .success(false)
                .message("Error processing your query: " + e.getMessage())
                .matches(List.of())
                .build();
        }
    }

    private String generateAIProcessedResponse(String userQuery, List<SummaryMatch> matches) {
        try {
            // Prepare context from matched summaries
            StringBuilder contextBuilder = new StringBuilder();
            contextBuilder.append("User Query: ").append(userQuery).append("\n\n");
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
                RESPONSE_GENERATION_SYSTEM_PROMPT, 
                userPrompt,
                0.3,  // Lower temperature for more factual responses
                1000  // Allow more tokens for detailed responses
            );

            if (aiResponse != null && !aiResponse.trim().isEmpty()) {
                log.debug("Generated AI-processed response for query: {}", userQuery);
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