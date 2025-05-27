package com.worksync.ai.service;

import com.worksync.ai.event.BaseEvent;
import com.worksync.ai.model.enums.Priority;
import com.worksync.ai.model.AlertEvent;
import com.worksync.ai.model.AppUsageEvent;
import com.worksync.ai.model.SecurityEvent;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for processing events based on their priority.
 * Handles the queuing and processing of events with different priority levels.
 */

public interface EventProcessingService {

    void processAppUsageEvent(AppUsageEvent event);

    void processSecurityEvent(SecurityEvent event);

    void processAlertEvent(AlertEvent event);
}