package com.worksync.ai.service;

import com.worksync.ai.model.AggregatedEventBundle;

/**
 * Service interface for generating natural language summaries of employee events using LLM.
 */
public interface LLMSummarizationService {
    /**
     * Generates a natural language summary of an employee's events using Spring AI's ChatModel.
     *
     * @param bundle The aggregated events for a single employee
     * @return A natural language summary of the employee's activities
     */
    String generateSummary(AggregatedEventBundle bundle);
} 