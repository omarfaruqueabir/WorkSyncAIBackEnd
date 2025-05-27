package com.worksync.ai.service;

import com.worksync.ai.model.dto.ChatbotResponse;
import com.worksync.ai.model.dto.ChatbotRequest;

public interface ChatbotService {
    /**
     * Process a natural language query and return relevant summaries
     * @param request The chatbot request containing the query
     * @return ChatbotResponse containing relevant summaries and metadata
     */
    ChatbotResponse processQuery(ChatbotRequest request);
} 