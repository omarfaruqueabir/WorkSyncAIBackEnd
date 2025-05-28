package com.worksync.ai.model.dto;

import com.worksync.ai.model.enums.QueryType;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class QueryAnalysis {
    private QueryType queryType;
    private List<String> filterKeywords;
    private Map<String, Object> extractionCriteria;
    private Map<String, Object> searchContext;
    private boolean requiresAggregation;
    private String timeframe;
    private String employeeId;
    private String employeeName;
    private List<String> requiredFields;
} 