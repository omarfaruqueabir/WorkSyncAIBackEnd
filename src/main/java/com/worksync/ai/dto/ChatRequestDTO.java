package com.worksync.ai.dto;

public record ChatRequestDTO(
    String question
) {
    public ChatRequestDTO {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("Question cannot be null or blank");
        }
    }
} 