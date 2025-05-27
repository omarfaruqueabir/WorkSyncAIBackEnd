package com.worksync.ai.controller;

import com.worksync.ai.dto.ChatRequestDTO;
import com.worksync.ai.dto.EventSummaryDTO;
import com.worksync.ai.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    @Autowired
    private ChatModel chatClient;

    @Autowired
    private EmbeddingModel embeddingClient;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Value("${chatbot.rag.fallback.similarity-threshold:0.7}")
    private double similarityThreshold;

    @Value("${chatbot.rag.fallback.message:I don't have enough data to answer that.}")
    private String fallbackMessage;

    @PostMapping("/query")
    public ResponseEntity<ApiResponse<String>> query(@RequestBody ChatRequestDTO request) {
        try {
            // Generate embedding for the question
            float[] questionEmbedding = embeddingClient.embed(request.question());

            // Search for similar summaries using cosine similarity
            CriteriaQuery query = new CriteriaQuery(new Criteria())
                .setPageable(PageRequest.of(0, 3));

            SearchHits<EventSummaryDTO> searchHits = elasticsearchOperations.search(
                query,
                EventSummaryDTO.class
            );

            // Filter summaries by similarity threshold
            List<String> relevantSummaries = searchHits.getSearchHits().stream()
                .filter(hit -> calculateCosineSimilarity(questionEmbedding, hit.getContent().embedding()) >= similarityThreshold)
                .map(SearchHit::getContent)
                .map(EventSummaryDTO::summaryText)
                .collect(Collectors.toList());

            // If no relevant summaries found, return fallback message
            if (relevantSummaries.isEmpty()) {
                return ResponseEntity.ok(new ApiResponse<>("SUCCESS", fallbackMessage, null));
            }

            // Build context from relevant summaries
            String context = String.join("\n\n", relevantSummaries);
            String prompt = String.format("""
                Context:
                %s
                
                Question: %s
                
                Answer:""", context, request.question());

            // Generate response using ChatGPT
            String response = chatClient.call(new Prompt(prompt))
                .getResult()
                .getOutput()
                .getText();

            return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "Query processed successfully", response));

        } catch (Exception e) {
            log.error("Error processing chatbot query: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>("ERROR", "Failed to process query: " + e.getMessage(), null));
        }
    }

    private double calculateCosineSimilarity(float[] embedding1, float[] embedding2) {
        if (embedding1.length != embedding2.length) {
            throw new IllegalArgumentException("Embedding dimensions do not match");
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < embedding1.length; i++) {
            dotProduct += embedding1[i] * embedding2[i];
            norm1 += embedding1[i] * embedding1[i];
            norm2 += embedding2[i] * embedding2[i];
        }

        norm1 = Math.sqrt(norm1);
        norm2 = Math.sqrt(norm2);

        return dotProduct / (norm1 * norm2);
    }
} 