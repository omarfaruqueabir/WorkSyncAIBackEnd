package com.worksync.ai.model.dto;

import com.worksync.ai.enums.EventType;

import java.time.LocalDateTime;
import java.util.Map;

public record EventAggregationDTO(
    String employeeId,
    String employeeName,
    EventType eventType,
    LocalDateTime timestamp,
    Map<String, Object> eventData
) {
    public EventAggregationDTO {
        if (employeeId == null || employeeId.isBlank()) {
            throw new IllegalArgumentException("Employee ID cannot be null or blank");
        }
        if (employeeName == null || employeeName.isBlank()) {
            throw new IllegalArgumentException("Employee name cannot be null or blank");
        }
        if (eventType == null) {
            throw new IllegalArgumentException("Event type cannot be null");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
        if (eventData == null) {
            throw new IllegalArgumentException("Event data cannot be null");
        }
    }
} 