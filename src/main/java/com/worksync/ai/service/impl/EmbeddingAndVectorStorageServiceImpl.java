package com.worksync.ai.service.impl;

import com.worksync.ai.client.OpenRouterClient;
import com.worksync.ai.model.entity.SummaryVector;
import com.worksync.ai.model.dto.SummaryMatch;
import com.worksync.ai.model.dto.VectorStoreRequest;
import com.worksync.ai.repository.SummaryVectorRepository;
import com.worksync.ai.service.EmbeddingAndVectorStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class EmbeddingAndVectorStorageServiceImpl implements EmbeddingAndVectorStorageService {

    @Autowired
    private OpenRouterClient openRouterClient;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    private SummaryVectorRepository summaryVectorRepository;

    @Override
    @Transactional
    public void embedAndStore(VectorStoreRequest request) {
        log.debug("Generating embedding for summary from employee: {}", request.getEmployeeId());

        try {
            // Generate embedding for the summary text using OpenRouter
            float[] embedding = openRouterClient.generateEmbedding(request.getSummary());
            
            if (embedding != null) {
                // Create and store the summary vector
                SummaryVector summaryVector = SummaryVector.builder()
                    .employeeId(request.getEmployeeId())
                    .summaryText(request.getSummary())
                    .embedding(embedding)
                    .timestamp(request.getTimestamp())
                    .build();

                summaryVectorRepository.save(summaryVector);
                log.debug("Successfully stored summary vector for employee: {}", request.getEmployeeId());
            } else {
                log.warn("Failed to generate embedding for employee {}, storing summary without embedding", request.getEmployeeId());
                // Store summary without embedding as fallback
                SummaryVector summaryVector = SummaryVector.builder()
                    .employeeId(request.getEmployeeId())
                    .summaryText(request.getSummary())
                    .embedding(new float[0]) // Empty embedding
                    .timestamp(request.getTimestamp())
                    .build();

                summaryVectorRepository.save(summaryVector);
            }

        } catch (Exception e) {
            log.error("Error storing summary vector for employee {}: {}", 
                request.getEmployeeId(), e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<SummaryMatch> similaritySearch(String query, int topK) {
        log.debug("Performing similarity search for query with topK: {}", topK);

        try {
            // Generate embedding for the query using OpenRouter
            float[] queryEmbedding = openRouterClient.generateEmbedding(query);
            
            if (queryEmbedding == null) {
                log.warn("Failed to generate embedding for search query");
                return List.of(); // Return empty list if embedding fails
            }

            // Retrieve all documents and calculate similarity in memory since we're using custom embeddings
            List<SummaryVector> allVectors = StreamSupport.stream(summaryVectorRepository.findAll().spliterator(), false)
                .collect(Collectors.toList());
            log.debug("Retrieved {} total vectors for similarity search", allVectors.size());

            // Calculate similarity for each vector and filter by threshold
            List<SummaryMatch> matches = allVectors.stream()
                .map(vector -> {
                    try {
                        if (vector.getEmbedding() == null || vector.getEmbedding().length == 0) {
                            log.warn("Skipping document with null or empty embedding for employee: {}", vector.getEmployeeId());
                            return null;
                        }
                        
                        double similarity = calculateCosineSimilarity(queryEmbedding, vector.getEmbedding());
                        log.debug("Similarity between query and employee {}: {}", vector.getEmployeeId(), similarity);
                        
                        // Only include matches above a reasonable threshold (adjust based on testing)
                        if (similarity > 0.1) { // Lower threshold for better recall
                            return new SummaryMatch(
                                vector.getEmployeeId(),
                                vector.getSummaryText(),
                                vector.getTimestamp() != null ? vector.getTimestamp() : LocalDateTime.now(),
                                similarity
                            );
                        }
                        return null;
                    } catch (Exception e) {
                        log.warn("Skipping malformed document during similarity search: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(match -> match != null) // Filter out null results
                .sorted((a, b) -> Double.compare(b.similarity(), a.similarity())) // Sort by similarity desc
                .limit(topK) // Limit to topK results
                .collect(Collectors.toList());

            log.debug("Found {} valid matches above similarity threshold for query", matches.size());
            return matches;

        } catch (Exception e) {
            log.error("Error performing similarity search: {}", e.getMessage(), e);
            return List.of(); // Return empty list instead of throwing exception
        }
    }

    @Override
    @Transactional
    public void storeSummaryEmbedding(String employeeId, String summaryText) {
        embedAndStore(VectorStoreRequest.builder()
            .employeeId(employeeId)
            .summary(summaryText)
            .timestamp(LocalDateTime.now())
            .build());
    }

    /**
     * Calculates cosine similarity between two embedding vectors
     */
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