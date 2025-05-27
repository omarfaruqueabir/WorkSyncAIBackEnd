package com.worksync.ai.scheduler;

import com.worksync.ai.service.EventSummaryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class SummaryGenerationScheduler {

    @Autowired
    private EventSummaryService summaryService;

    @Value("${summary.generation.interval:1800}")
    private long summaryGenerationInterval;

    @Scheduled(fixedRateString = "${summary.generation.interval:1800}000")
    public void generateSummaries() {
        try {
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusHours(24);

            log.info("Starting summary generation for period: {} to {}", startTime, endTime);

            summaryService.generateEmployeeSummaries(startTime, endTime)
                .thenAccept(summaries -> 
                    log.info("Successfully generated {} summaries", summaries.size()))
                .exceptionally(e -> {
                    log.error("Error generating summaries: {}", e.getMessage(), e);
                    return null;
                });
        } catch (Exception e) {
            log.error("Error in summary generation scheduler: {}", e.getMessage(), e);
        }
    }
} 