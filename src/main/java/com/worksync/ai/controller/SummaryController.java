package com.worksync.ai.controller;

import com.worksync.ai.response.ApiResponse;
import com.worksync.ai.service.SummaryPipelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/summary")
public class SummaryController {

    @Autowired
    private SummaryPipelineService summaryPipelineService;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<Void>> generateSummary() {
        log.info("Manually triggering summary generation");
        try {
            summaryPipelineService.runHourlySummary();
            return ResponseEntity.ok(new ApiResponse<>(
                "SUCCESS",
                "Summary generation completed successfully",
                null
            ));
        } catch (Exception e) {
            log.error("Error during manual summary generation: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                "ERROR",
                "Failed to generate summary: " + e.getMessage(),
                null
            ));
        }
    }
} 