package com.worksync.ai.service.impl;

import com.worksync.ai.entity.EventAggregation;
import com.worksync.ai.enums.EventType;
import com.worksync.ai.model.AppUsageEvent;
import com.worksync.ai.model.AlertEvent;
import com.worksync.ai.model.SecurityEvent;
import com.worksync.ai.model.AggregatedEventBundle;
import com.worksync.ai.model.dto.EventAggregationDTO;
import com.worksync.ai.repository.EventAggregationRepository;
import com.worksync.ai.repository.AlertEventRepository;
import com.worksync.ai.repository.AppUsageEventRepository;
import com.worksync.ai.repository.SecurityEventRepository;
import com.worksync.ai.service.EventAggregationService;
import com.worksync.ai.service.LLMSummarizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EventAggregationServiceImpl implements EventAggregationService {

    @Autowired
    private EventAggregationRepository aggregationRepository;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    private LLMSummarizationService summarizationService;

    @Autowired
    private AppUsageEventRepository appUsageEventRepository;

    @Autowired
    private SecurityEventRepository securityEventRepository;

    @Autowired
    private AlertEventRepository alertEventRepository;

    @Override
    @Transactional
    public void aggregateAppUsageEvents(List<AppUsageEvent> events, LocalDateTime startTime, LocalDateTime endTime) {
        if (events.isEmpty()) {
            return;
        }

        // Group events by employee
        Map<String, List<AppUsageEvent>> eventsByEmployee = events.stream()
            .collect(Collectors.groupingBy(AppUsageEvent::getEmployeeId));

        eventsByEmployee.forEach((employeeId, employeeEvents) -> {
            // Create aggregation data
            Map<String, Object> aggregatedData = new HashMap<>();
            
            // Aggregate app usage durations
            Map<String, Long> appDurations = employeeEvents.stream()
                .collect(Collectors.groupingBy(
                    AppUsageEvent::getAppName,
                    Collectors.summingLong(AppUsageEvent::getDurationInSeconds)
                ));
            aggregatedData.put("appDurations", appDurations);

            // Create and save aggregation
            EventAggregation aggregation = new EventAggregation();
            aggregation.setEmployeeId(employeeId);
            aggregation.setEmployeeName(employeeEvents.get(0).getEmployeeName());
            aggregation.setEventType(EventType.APP_USAGE);
            aggregation.setStartTime(startTime);
            aggregation.setEndTime(endTime);
            aggregation.setAggregatedData(aggregatedData);
            aggregation.setCreatedAt(LocalDateTime.now());
            aggregation.setUpdatedAt(LocalDateTime.now());

            aggregationRepository.save(aggregation);
            log.info("Saved app usage aggregation for employee: {}", employeeId);
        });
    }

    @Override
    @Transactional
    public void aggregateSecurityEvents(List<SecurityEvent> events, LocalDateTime startTime, LocalDateTime endTime) {
        if (events.isEmpty()) {
            return;
        }

        // Group events by employee
        Map<String, List<SecurityEvent>> eventsByEmployee = events.stream()
            .collect(Collectors.groupingBy(SecurityEvent::getEmployeeId));

        eventsByEmployee.forEach((employeeId, employeeEvents) -> {
            // Create aggregation data
            Map<String, Object> aggregatedData = new HashMap<>();
            
            // Aggregate security events by threat type
            Map<String, Long> threatCounts = employeeEvents.stream()
                .collect(Collectors.groupingBy(
                    SecurityEvent::getThreatType,
                    Collectors.counting()
                ));
            aggregatedData.put("threatCounts", threatCounts);

            // Create and save aggregation
            EventAggregation aggregation = new EventAggregation();
            aggregation.setEmployeeId(employeeId);
            aggregation.setEmployeeName(employeeEvents.get(0).getEmployeeName());
            aggregation.setEventType(EventType.SECURITY);
            aggregation.setStartTime(startTime);
            aggregation.setEndTime(endTime);
            aggregation.setAggregatedData(aggregatedData);
            aggregation.setCreatedAt(LocalDateTime.now());
            aggregation.setUpdatedAt(LocalDateTime.now());

            aggregationRepository.save(aggregation);
            log.info("Saved security aggregation for employee: {}", employeeId);
        });
    }

    @Override
    @Transactional
    public void aggregateAlertEvents(List<AlertEvent> events, LocalDateTime startTime, LocalDateTime endTime) {
        if (events.isEmpty()) {
            return;
        }

        // Group events by employee
        Map<String, List<AlertEvent>> eventsByEmployee = events.stream()
            .collect(Collectors.groupingBy(AlertEvent::getEmployeeId));

        eventsByEmployee.forEach((employeeId, employeeEvents) -> {
            // Create aggregation data
            Map<String, Object> aggregatedData = new HashMap<>();
            
            // Aggregate alerts by type
            Map<String, List<String>> alertsByType = employeeEvents.stream()
                .collect(Collectors.groupingBy(
                    AlertEvent::getAlertType,
                    Collectors.mapping(AlertEvent::getDescription, Collectors.toList())
                ));
            aggregatedData.put("alertsByType", alertsByType);

            // Create and save aggregation
            EventAggregation aggregation = new EventAggregation();
            aggregation.setEmployeeId(employeeId);
            aggregation.setEmployeeName(employeeEvents.get(0).getEmployeeName());
            aggregation.setEventType(EventType.ALERT);
            aggregation.setStartTime(startTime);
            aggregation.setEndTime(endTime);
            aggregation.setAggregatedData(aggregatedData);
            aggregation.setCreatedAt(LocalDateTime.now());
            aggregation.setUpdatedAt(LocalDateTime.now());

            aggregationRepository.save(aggregation);
            log.info("Saved alert aggregation for employee: {}", employeeId);
        });
    }

    @Override
    public Map<String, AggregatedEventBundle> fetchEventsForLastHour() {
        log.debug("Fetching events for the last hour");
        
        // Calculate time range
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(3600); // 1 hour ago
        
        // Create criteria for time range
        Criteria timeRangeCriteria = new Criteria("timestamp")
            .greaterThanEqual(startTime)
            .lessThanEqual(endTime);

        // Fetch events by type
        List<AppUsageEvent> appUsageEvents = fetchEvents(AppUsageEvent.class, timeRangeCriteria);
        List<SecurityEvent> securityEvents = fetchEvents(SecurityEvent.class, timeRangeCriteria);
        List<AlertEvent> alertEvents = fetchEvents(AlertEvent.class, timeRangeCriteria);

        log.debug("Found {} app usage events, {} security events, and {} alert events",
            appUsageEvents.size(), securityEvents.size(), alertEvents.size());

        // Group events by employeeId
        Map<String, AggregatedEventBundle> aggregatedEvents = new HashMap<>();

        // Process app usage events
        appUsageEvents.forEach(event -> {
            AggregatedEventBundle bundle = aggregatedEvents.computeIfAbsent(event.getEmployeeId(), employeeId -> 
                new AggregatedEventBundle(employeeId, event.getEmployeeName())
            );
            bundle.getAppUsageEvents().add(event);
            
            // Collect additional properties
            if (event.getPcId() != null) {
                bundle.getPcIds().add(event.getPcId());
            }
            if (event.getCategory() != null) {
                bundle.getCategories().add(event.getCategory());
            }
        });

        // Process security events
        securityEvents.forEach(event -> {
            AggregatedEventBundle bundle = aggregatedEvents.computeIfAbsent(event.getEmployeeId(), employeeId -> 
                new AggregatedEventBundle(employeeId, event.getEmployeeName())
            );
            bundle.getSecurityEvents().add(event);
            
            // Collect additional properties
            if (event.getPcId() != null) {
                bundle.getPcIds().add(event.getPcId());
            }
            if (event.getCategory() != null) {
                bundle.getCategories().add(event.getCategory());
            }
        });

        // Process alert events
        alertEvents.forEach(event -> {
            AggregatedEventBundle bundle = aggregatedEvents.computeIfAbsent(event.getEmployeeId(), employeeId -> 
                new AggregatedEventBundle(employeeId, event.getEmployeeName())
            );
            bundle.getAlertEvents().add(event);
            
            // Collect additional properties
            if (event.getPcId() != null) {
                bundle.getPcIds().add(event.getPcId());
            }
            if (event.getCategory() != null) {
                bundle.getCategories().add(event.getCategory());
            }
        });

        log.debug("Aggregated events for {} employees", aggregatedEvents.size());
        return aggregatedEvents;
    }

    private <T> List<T> fetchEvents(Class<T> eventType, Criteria timeRangeCriteria) {
        CriteriaQuery query = new CriteriaQuery(timeRangeCriteria);
        SearchHits<T> searchHits = elasticsearchOperations.search(query, eventType);
        return searchHits.stream()
            .map(hit -> hit.getContent())
            .collect(Collectors.toList());
    }

    public void processEmployeeEvents(AggregatedEventBundle bundle) {
        String summary = summarizationService.generateSummary(bundle);
        // Use the summary (e.g., store it, send it in a report, etc.)
    }

    @Override
    public Map<String, List<EventAggregationDTO>> getEventsByTimeRangeGroupedByEmployee(
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        log.debug("Fetching events between {} and {}", startTime, endTime);

        // Fetch all events for the time range
        List<AppUsageEvent> appUsageEvents = appUsageEventRepository.findByTimestampBetween(startTime, endTime);
        List<SecurityEvent> securityEvents = securityEventRepository.findByTimestampBetween(startTime, endTime);
        List<AlertEvent> alertEvents = alertEventRepository.findByTimestampBetween(startTime, endTime);

        // Create a map to store events by employee
        Map<String, List<EventAggregationDTO>> eventsByEmployee = new HashMap<>();

        // Process app usage events
        appUsageEvents.forEach(event -> {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("appName", event.getAppName());
            eventData.put("durationInSeconds", event.getDurationInSeconds());
            eventData.put("pcId", event.getPcId());
            eventData.put("category", event.getCategory());

            EventAggregationDTO dto = new EventAggregationDTO(
                event.getEmployeeId(),
                event.getEmployeeName(),
                EventType.APP_USAGE,
                event.getTimestamp(),
                eventData
            );

            eventsByEmployee.computeIfAbsent(event.getEmployeeId(), k -> new ArrayList<>()).add(dto);
        });

        // Process security events
        securityEvents.forEach(event -> {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("threatType", event.getThreatType());
            eventData.put("url", event.getUrl());
            eventData.put("pcId", event.getPcId());
            eventData.put("category", event.getCategory());

            EventAggregationDTO dto = new EventAggregationDTO(
                event.getEmployeeId(),
                event.getEmployeeName(),
                EventType.SECURITY,
                event.getTimestamp(),
                eventData
            );

            eventsByEmployee.computeIfAbsent(event.getEmployeeId(), k -> new ArrayList<>()).add(dto);
        });

        // Process alert events
        alertEvents.forEach(event -> {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("alertType", event.getAlertType());
            eventData.put("severity", event.getSeverity());
            eventData.put("pcId", event.getPcId());
            eventData.put("category", event.getCategory());

            EventAggregationDTO dto = new EventAggregationDTO(
                event.getEmployeeId(),
                event.getEmployeeName(),
                EventType.ALERT,
                event.getTimestamp(),
                eventData
            );

            eventsByEmployee.computeIfAbsent(event.getEmployeeId(), k -> new ArrayList<>()).add(dto);
        });

        // Sort events by timestamp for each employee
        eventsByEmployee.forEach((employeeId, events) -> {
            events.sort(Comparator.comparing(EventAggregationDTO::timestamp));
        });

        log.debug("Found events for {} employees", eventsByEmployee.size());
        return eventsByEmployee;
    }
} 