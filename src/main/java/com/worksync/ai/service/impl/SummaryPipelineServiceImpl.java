package com.worksync.ai.service.impl;

import com.worksync.ai.model.AggregatedEventBundle;
import com.worksync.ai.model.AppUsageEvent;
import com.worksync.ai.model.dto.EventAggregationDTO;
import com.worksync.ai.model.dto.SummaryRequest;
import com.worksync.ai.model.dto.VectorStoreRequest;
import com.worksync.ai.model.AlertEvent;
import com.worksync.ai.model.SecurityEvent;
import com.worksync.ai.service.EventAggregationService;
import com.worksync.ai.service.LLMSummarizationService;
import com.worksync.ai.service.EmbeddingAndVectorStorageService;
import com.worksync.ai.service.SummaryPipelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Slf4j
@Service
public class SummaryPipelineServiceImpl implements SummaryPipelineService {

    @Value("${summary.generation.enabled:true}")
    private boolean summaryGenerationEnabled;

    @Autowired
    private EventAggregationService eventAggregationService;

    @Autowired
    private LLMSummarizationService llmSummarizationService;

    @Autowired
    private EmbeddingAndVectorStorageService embeddingAndVectorStorageService;

    /**
     * Runs hourly to generate and store summaries of employee activities
     * Scheduled to run at the start of every hour
     */
    @Override
    @Scheduled(cron = "0 0 * * * *") // Run at the start of every hour
    @Transactional
    public void runHourlySummary() {
        if (!summaryGenerationEnabled) {
            log.info("Summary generation is disabled. Skipping scheduled execution.");
            return;
        }

        // Use current date but format it to match Elasticsearch
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.minusHours(1);
        LocalDateTime endTime = now;
        
        log.info("Starting hourly summary generation for period: {} to {}", startTime, endTime);
        
        try {
            // Fetch events grouped by employee
            Map<String, List<EventAggregationDTO>> employeeEvents = eventAggregationService
                .getEventsByTimeRangeGroupedByEmployee(startTime, endTime);

            log.info("Retrieved events for {} employees", employeeEvents.size());
            log.info("Retrieved events for {} employees", employeeEvents
            );

            // Process each employee's events
            employeeEvents.forEach((employeeId, events) -> {
                try {
                    processSingleEmployeeSummary(employeeId, events, startTime, endTime);
                } catch (Exception e) {
                    log.error("Error processing summary for employee {}: {}", employeeId, e.getMessage(), e);
                }
            });

            log.info("Successfully completed hourly summary generation");
            
        } catch (Exception e) {
            log.error("Error during hourly summary generation: {}", e.getMessage(), e);
            throw e; // Rethrowing to ensure transaction rollback
        }
    }

    /**
     * Processes summary generation for a single employee
     */
    private void processSingleEmployeeSummary(String employeeId, 
                                            List<EventAggregationDTO> events,
                                            LocalDateTime startTime,
                                            LocalDateTime endTime) {
        log.debug("Processing summary for employee {} with {} events", employeeId, events.size());

        try {
            // Convert EventAggregationDTO list to AggregatedEventBundle
            AggregatedEventBundle bundle = convertToEventBundle(events);
            bundle.setEmployeeId(employeeId);

            // Generate text summary using LLM
            String summary = llmSummarizationService.generateSummary(bundle);

            log.debug("Generated summary for employee {}: {}", employeeId, summary);

            // Generate embedding and store in vector store
            embeddingAndVectorStorageService.embedAndStore(
                VectorStoreRequest.builder()
                    .employeeId(employeeId)
                    .summary(summary)
                    .timestamp(endTime)
                    .build()
            );

            log.debug("Successfully stored summary embedding for employee {}", employeeId);
        } catch (Exception e) {
            log.error("Error processing summary for employee {}: {}", employeeId, e.getMessage());
            throw e;
        }
    }

    /**
     * Converts a list of EventAggregationDTO to AggregatedEventBundle
     */
    private AggregatedEventBundle convertToEventBundle(List<EventAggregationDTO> events) {
        if (events.isEmpty()) {
            return new AggregatedEventBundle();
        }

        // Get employee info from the first event
        EventAggregationDTO firstEvent = events.get(0);
        AggregatedEventBundle bundle = new AggregatedEventBundle(
            firstEvent.employeeId(), 
            firstEvent.employeeName()
        );

        events.forEach(event -> {
            switch (event.eventType()) {
                case APP_USAGE -> {
                    AppUsageEvent appUsageEvent = new AppUsageEvent();
                    appUsageEvent.setEmployeeId(event.employeeId());
                    appUsageEvent.setEmployeeName(event.employeeName());
                    appUsageEvent.setTimestamp(event.timestamp());
                    
                    // Set AppUsageEvent specific properties
                    appUsageEvent.setAppName((String) event.eventData().get("appName"));
                    
                    // Handle potential null or wrong type for durationInSeconds
                    Object duration = event.eventData().get("durationInSeconds");
                    if (duration != null) {
                        if (duration instanceof Long) {
                            appUsageEvent.setDurationInSeconds((Long) duration);
                        } else if (duration instanceof Integer) {
                            appUsageEvent.setDurationInSeconds(((Integer) duration).longValue());
                        }
                    }
                    
                    // Collect additional properties
                    String pcId = (String) event.eventData().get("pcId");
                    if (pcId != null) {
                        bundle.getPcIds().add(pcId);
                        appUsageEvent.setPcId(pcId);
                    }
                    
                    String category = (String) event.eventData().get("category");
                    if (category != null) {
                        bundle.getCategories().add(category);
                        appUsageEvent.setCategory(category);
                    }
                    
                    bundle.getAppUsageEvents().add(appUsageEvent);
                }
                case SECURITY -> {
                    SecurityEvent securityEvent = new SecurityEvent();
                    securityEvent.setEmployeeId(event.employeeId());
                    securityEvent.setEmployeeName(event.employeeName());
                    securityEvent.setTimestamp(event.timestamp());
                    
                    // Set SecurityEvent specific properties
                    securityEvent.setThreatType((String) event.eventData().get("threatType"));
                    securityEvent.setUrl((String) event.eventData().get("url"));
                    
                    // Collect additional properties
                    String pcId = (String) event.eventData().get("pcId");
                    if (pcId != null) {
                        bundle.getPcIds().add(pcId);
                        securityEvent.setPcId(pcId);
                    }
                    
                    String category = (String) event.eventData().get("category");
                    if (category != null) {
                        bundle.getCategories().add(category);
                        securityEvent.setCategory(category);
                    }
                    
                    bundle.getSecurityEvents().add(securityEvent);
                }
                case ALERT -> {
                    AlertEvent alertEvent = new AlertEvent();
                    alertEvent.setEmployeeId(event.employeeId());
                    alertEvent.setEmployeeName(event.employeeName());
                    alertEvent.setTimestamp(event.timestamp());
                    
                    // Set AlertEvent specific properties
                    alertEvent.setAlertType((String) event.eventData().get("alertType"));
                    alertEvent.setSeverity((String) event.eventData().get("severity"));
                    
                    // Collect additional properties
                    String pcId = (String) event.eventData().get("pcId");
                    if (pcId != null) {
                        bundle.getPcIds().add(pcId);
                        alertEvent.setPcId(pcId);
                    }
                    
                    String category = (String) event.eventData().get("category");
                    if (category != null) {
                        bundle.getCategories().add(category);
                        alertEvent.setCategory(category);
                    }
                    
                    bundle.getAlertEvents().add(alertEvent);
                }
            }
        });

        return bundle;
    }
} 