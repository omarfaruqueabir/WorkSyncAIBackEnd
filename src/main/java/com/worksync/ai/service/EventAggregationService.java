package com.worksync.ai.service;

import com.worksync.ai.model.AppUsageEvent;
import com.worksync.ai.model.AlertEvent;
import com.worksync.ai.model.SecurityEvent;
import com.worksync.ai.model.AggregatedEventBundle;
import com.worksync.ai.model.dto.EventAggregationDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Service interface for aggregating different types of events.
 * Handles the aggregation of app usage, security, and alert events.
 */
public interface EventAggregationService {

    void aggregateAppUsageEvents(List<AppUsageEvent> events, LocalDateTime startTime, LocalDateTime endTime);

    void aggregateSecurityEvents(List<SecurityEvent> events, LocalDateTime startTime, LocalDateTime endTime);

    void aggregateAlertEvents(List<AlertEvent> events, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Fetches all events from the last hour and groups them by employeeId.
     * This includes AppUsageEvents, SecurityEvents, and AlertEvents.
     *
     * @return A map where the key is the employeeId and the value is an AggregatedEventBundle
     *         containing all events for that employee from the last hour
     */
    Map<String, AggregatedEventBundle> fetchEventsForLastHour();

    /**
     * Retrieves events within a time range and groups them by employee
     *
     * @param startTime The start of the time range (inclusive)
     * @param endTime The end of the time range (exclusive)
     * @return Map of employee IDs to their list of events
     */
    Map<String, List<EventAggregationDTO>> getEventsByTimeRangeGroupedByEmployee(
        LocalDateTime startTime, 
        LocalDateTime endTime
    );
} 