package com.worksync.ai.dto;

import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import java.time.LocalDateTime;
import java.util.Map;

@Document(indexName = "#{@indexNameProvider.getIndexName()}")
public record AppUsageAggregationDTO(
    @Field(type = FieldType.Keyword)
    String employeeId,
    
    @Field(type = FieldType.Keyword)
    String employeeName,
    
    @Field(type = FieldType.Date)
    LocalDateTime timestamp,
    
    @Field(type = FieldType.Object)
    Map<String, Long> appDurations,
    
    @Field(type = FieldType.Long)
    long totalDuration
) {
    public AppUsageAggregationDTO {
        if (employeeId == null || employeeId.isBlank()) {
            throw new IllegalArgumentException("Employee ID cannot be null or blank");
        }
        if (employeeName == null || employeeName.isBlank()) {
            throw new IllegalArgumentException("Employee name cannot be null or blank");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
        if (appDurations == null || appDurations.isEmpty()) {
            throw new IllegalArgumentException("App durations cannot be null or empty");
        }
    }
} 