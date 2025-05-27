package com.worksync.ai.model.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChatbotResponse {
    private boolean success;
    private String message;
    private List<SummaryMatch> matches;
} 