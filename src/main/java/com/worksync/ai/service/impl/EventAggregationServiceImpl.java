package com.worksync.ai.service.impl;

import com.worksync.ai.entity.EventAggregation;
import com.worksync.ai.enums.EventType;
import com.worksync.ai.model.AlertEvent;
import com.worksync.ai.model.AppUsageEvent;
import com.worksync.ai.model.SecurityEvent;
import com.worksync.ai.repository.EventAggregationRepository;
import com.worksync.ai.service.EventAggregationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EventAggregationServiceImpl implements EventAggregationService {

    @Autowired
    private EventAggregationRepository aggregationRepository;

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
} 