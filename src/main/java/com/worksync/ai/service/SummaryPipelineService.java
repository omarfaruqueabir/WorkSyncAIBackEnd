package com.worksync.ai.service;

public interface SummaryPipelineService {
    /**
     * Runs hourly to generate and store summaries of employee activities.
     * This method:
     * 1. Fetches events from the last hour
     * 2. Groups them by employee
     * 3. Generates summaries using LLM
     * 4. Stores the summaries with embeddings
     */
    void runHourlySummary();
} 