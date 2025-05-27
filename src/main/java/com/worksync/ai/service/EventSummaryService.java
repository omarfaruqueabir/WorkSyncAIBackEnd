package com.worksync.ai.service;

import com.worksync.ai.dto.EventSummaryDTO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface EventSummaryService {
    CompletableFuture<List<EventSummaryDTO>> generateEmployeeSummaries(LocalDateTime startTime, LocalDateTime endTime);
    CompletableFuture<EventSummaryDTO> generateEmployeeSummary(String employeeId, LocalDateTime startTime, LocalDateTime endTime);
} 