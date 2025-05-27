package com.worksync.ai.service;

import com.worksync.ai.model.dto.QueryAnalysis;

public interface QueryAnalyzerService {
    /**
     * Analyzes a user query to determine the type of query and required processing
     * @param query The user's natural language query
     * @return QueryAnalysis containing query type and processing requirements
     */
    QueryAnalysis analyzeQuery(String query);
} 