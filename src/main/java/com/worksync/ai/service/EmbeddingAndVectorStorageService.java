package com.worksync.ai.service;

import com.worksync.ai.model.dto.SummaryMatch;
import com.worksync.ai.model.dto.VectorStoreRequest;

import java.util.List;

/**
 * Service interface for managing embeddings and vector storage operations.
 */
public interface EmbeddingAndVectorStorageService {
    /**
     * Embeds the summary text and stores it in the vector store
     * @param request The request containing summary and metadata
     */
    void embedAndStore(VectorStoreRequest request);

    /**
     * Performs similarity search against stored vectors
     * @param query The query text to search for
     * @param topK The number of most similar results to return
     * @return List of matching summaries with similarity scores
     */
    List<SummaryMatch> similaritySearch(String query, int topK);

    /**
     * Stores the embedding of a summary text for an employee in the vector store.
     *
     * @param employeeId The ID of the employee
     * @param summaryText The summary text to be embedded and stored
     */
    void storeSummaryEmbedding(String employeeId, String summaryText);
} 