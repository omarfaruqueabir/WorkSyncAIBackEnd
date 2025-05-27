package com.worksync.ai.service;

import com.worksync.ai.model.AlertEvent;
import com.worksync.ai.model.AppUsageEvent;
import com.worksync.ai.model.SecurityEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for aggregating different types of events.
 * Handles the aggregation of app usage, security, and alert events.
 */
public interface EventAggregationService {

    void aggregateAppUsageEvents(List<AppUsageEvent> events, LocalDateTime startTime, LocalDateTime endTime);

    void aggregateSecurityEvents(List<SecurityEvent> events, LocalDateTime startTime, LocalDateTime endTime);

    void aggregateAlertEvents(List<AlertEvent> events, LocalDateTime startTime, LocalDateTime endTime);
} 