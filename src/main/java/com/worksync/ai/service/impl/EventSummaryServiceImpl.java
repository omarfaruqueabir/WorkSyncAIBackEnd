package com.worksync.ai.service.impl;

import com.worksync.ai.client.OpenRouterClient;
import com.worksync.ai.dto.AppUsageAggregationDTO;
import com.worksync.ai.dto.SecurityAggregationDTO;
import com.worksync.ai.dto.AlertAggregationDTO;
import com.worksync.ai.dto.EventSummaryDTO;
import com.worksync.ai.entity.EventSummary;
import com.worksync.ai.repository.EventAggregationRepository;
import com.worksync.ai.repository.EventSummaryRepository;
import com.worksync.ai.service.EventSummaryService;
import com.worksync.ai.mapper.EventAggregationMapper;
import com.worksync.ai.mapper.EventSummaryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EventSummaryServiceImpl implements EventSummaryService {

    @Autowired
    private EventAggregationRepository aggregationRepository;

    @Autowired
    private EventSummaryRepository summaryRepository;

    @Autowired
    private OpenRouterClient openRouterClient;

    @Autowired
    private EventAggregationMapper aggregationMapper;

    @Autowired
    private EventSummaryMapper summaryMapper;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Async
    @Override
    public CompletableFuture<List<EventSummaryDTO>> generateEmployeeSummaries(LocalDateTime startTime, LocalDateTime endTime) {
        try {
            // TODO: Implement logic to get all employee IDs
            Set<String> employeeIds = new HashSet<>(); // Replace with actual implementation
            
            List<CompletableFuture<EventSummaryDTO>> futures = employeeIds.stream()
                .map(employeeId -> generateEmployeeSummary(employeeId, startTime, endTime))
                .collect(Collectors.toList());

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            log.error("Error generating employee summaries: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async
    @Override
    public CompletableFuture<EventSummaryDTO> generateEmployeeSummary(String employeeId, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            // Build context from aggregated data
            StringBuilder context = new StringBuilder();
            String employeeName = "Unknown"; // Will be updated from aggregations

            // Add app usage summary
            var appUsageEntity = aggregationRepository.findById(employeeId).orElse(null);
            var appUsage = aggregationMapper.toAppUsageDTO(appUsageEntity);
            if (appUsage != null) {
                employeeName = appUsage.employeeName();
                context.append("App Usage:\n");
                appUsage.appDurations().forEach((app, duration) -> {
                    String formattedDuration = formatDuration(duration);
                    context.append(String.format("- %s: %s\n", app, formattedDuration));
                });
            }

            // Add security events summary
            var securityEntity = aggregationRepository.findById(employeeId).orElse(null);
            var security = aggregationMapper.toSecurityDTO(securityEntity);
            if (security != null) {
                context.append("\nSecurity Events:\n");
                security.threatCounts().forEach((threatType, count) -> 
                    context.append(String.format("- %s: %d occurrences\n", threatType, count)));
            }

            // Add alerts summary
            var alertsEntity = aggregationRepository.findById(employeeId).orElse(null);
            var alerts = aggregationMapper.toAlertDTO(alertsEntity);
            if (alerts != null) {
                context.append("\nAlerts:\n");
                alerts.alertsByType().forEach((alertType, alertList) -> {
                    context.append(String.format("- %s: %d alerts\n", alertType, alertList.size()));
                    alertList.forEach(alert -> context.append(String.format("  * %s\n", alert)));
                });
            }

            // Generate summary using OpenRouter
            String systemPrompt = "Analyze the following employee activity data and provide a concise, professional summary. Focus on key patterns, potential concerns, and notable achievements.";
            String userPrompt = String.format("""
                Employee: %s
                Time Period: %s to %s
                
                %s
                
                Summary:""",
                employeeName,
                startTime.format(TIME_FORMATTER),
                endTime.format(TIME_FORMATTER),
                context.toString());

            String summaryText = openRouterClient.chatCompletion(systemPrompt, userPrompt);
            if (summaryText == null) {
                summaryText = "Summary generation failed for employee " + employeeName;
            }

            // Generate embedding for the summary
            float[] embedding = openRouterClient.generateEmbedding(summaryText);
            if (embedding == null) {
                embedding = new float[0]; // Empty embedding as fallback
            }

            // Create DTO
            EventSummaryDTO dto = new EventSummaryDTO(
                employeeId,
                "EMPLOYEE",
                LocalDateTime.now(),
                summaryText,
                embedding
            );

            // Convert to entity and save
            EventSummary entity = summaryMapper.toEntity(dto);
            EventSummary savedEntity = summaryRepository.save(entity);

            // Convert back to DTO and return
            return CompletableFuture.completedFuture(summaryMapper.toDto(savedEntity));
        } catch (Exception e) {
            log.error("Error generating summary for employee {}: {}", employeeId, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private String formatDuration(long seconds) {
        Duration duration = Duration.ofSeconds(seconds);
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }
} 