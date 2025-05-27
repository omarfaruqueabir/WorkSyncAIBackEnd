package com.worksync.ai.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SummaryRequest {
    private String employeeId;
    private List<EventAggregationDTO> events;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
} 