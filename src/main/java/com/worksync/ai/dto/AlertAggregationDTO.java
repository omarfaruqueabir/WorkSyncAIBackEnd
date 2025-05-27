package com.worksync.ai.dto;

import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(indexName = "#{@indexNameProvider.getIndexName()}")
public record AlertAggregationDTO(
    @Field(type = FieldType.Keyword)
    String employeeId,
    
    @Field(type = FieldType.Keyword)
    String employeeName,
    
    @Field(type = FieldType.Date)
    LocalDateTime timestamp,
    
    @Field(type = FieldType.Object)
    Map<String, List<String>> alertsByType,
    
    @Field(type = FieldType.Integer)
    int totalAlerts
) {
    public AlertAggregationDTO {
        if (employeeId == null || employeeId.isBlank()) {
            throw new IllegalArgumentException("Employee ID cannot be null or blank");
        }
        if (employeeName == null || employeeName.isBlank()) {
            throw new IllegalArgumentException("Employee name cannot be null or blank");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
        if (alertsByType == null || alertsByType.isEmpty()) {
            throw new IllegalArgumentException("Alerts by type cannot be null or empty");
        }
    }
} 