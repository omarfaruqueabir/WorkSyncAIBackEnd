package com.worksync.ai.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatbotRequest(
    @NotBlank(message = "Query cannot be blank")
    @Size(min = 3, max = 1000, message = "Query must be between 3 and 1000 characters")
    String query,
    
    Integer topK
) {
    public ChatbotRequest {
        if (topK != null && (topK < 1 || topK > 20)) {
            throw new IllegalArgumentException("topK must be between 1 and 20");
        }
    }
} 