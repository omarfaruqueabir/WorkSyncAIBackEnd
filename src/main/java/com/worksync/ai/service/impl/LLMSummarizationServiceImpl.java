package com.worksync.ai.service.impl;

import com.worksync.ai.client.OllamaClient;
import com.worksync.ai.model.AggregatedEventBundle;
import com.worksync.ai.model.AlertEvent;
import com.worksync.ai.model.AppUsageEvent;
import com.worksync.ai.model.SecurityEvent;
import com.worksync.ai.service.LLMSummarizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LLMSummarizationServiceImpl implements LLMSummarizationService {

    @Autowired
    private OllamaClient ollamaClient;

    @Value("${ollama.model:llama2}")
    private String model;

    private static final String SYSTEM_PROMPT = 
        "You are an expert at creating comprehensive, detailed summaries of employee computer activity. " +
        "For each event, mention every property (including url, threatType, and all metadata fields) and explain what it means. " +
        "Your role is to provide a thorough analysis of what the employee did during their work session, " +
        "including ALL available details such as: applications used with duration, categories, device information, " +
        "timestamps, priorities, security events with URLs and threat types, alerts with severity levels, " +
        "and any metadata or context provided. " +
        "Write in a professional, detailed manner that captures every aspect of the employee's activity. " +
        "Include specific metrics, times, application names, categories, device IDs, security details including URLs, " +
        "alert information, and all other available information to provide a complete picture of the work session. " +
        "Focus on being comprehensive and factual, mentioning every significant detail available in the data. " +
        "Do not include summary numbers or numerical references - focus on the actual content and activities.";

    @Override
    public String generateSummary(AggregatedEventBundle bundle) {
        log.debug("Generating comprehensive summary for employee: {}", bundle.getEmployeeId());

        // Build comprehensive activity data with ALL details
        String activityData = buildComprehensiveActivityNarrative(bundle);

        // Build the user prompt with detailed instructions
        String userPrompt = String.format("""
            Create a comprehensive summary of %s's work activity based on the following detailed data:

            %s

            Write a thorough, professional summary that includes:
            - ALL applications used with specific durations and categories
            - Device/PC information and identifiers
            - Timestamp details and session timing
            - Complete security event information including URLs, threat types, and descriptions
            - Full alert details with types, severity levels, and descriptions
            - Priority levels of events and their significance
            - Any metadata or additional context provided
            - Categories and classifications of activities
            
            Provide a comprehensive analysis that captures every available detail to give a complete 
            picture of the employee's work session. Include specific metrics, exact application names, 
            timing information, and all security/alert details including URLs when available. 
            Make it thorough and informative. Do not include any summary numbering or numerical labels 
            - focus purely on the activity content and details.""",
            bundle.getEmployeeName() != null ? bundle.getEmployeeName() : ("Employee " + bundle.getEmployeeId()),
            activityData
        );

        // Generate summary using Ollama
        try {
            String summary = ollamaClient.generateCompletion(SYSTEM_PROMPT, userPrompt);
            
            if (summary != null && !summary.trim().isEmpty()) {
                log.debug("Generated comprehensive summary for employee {}: {} chars", 
                    bundle.getEmployeeId(), summary.length());
                return summary.trim();
            } else {
                log.warn("Ollama returned empty response for employee {}", bundle.getEmployeeId());
                return generateComprehensiveFallbackSummary(bundle);
            }
        } catch (Exception e) {
            log.error("Error generating AI summary for employee {}: {}", bundle.getEmployeeId(), e.getMessage(), e);
            return generateComprehensiveFallbackSummary(bundle);
        }
    }

    private String buildComprehensiveActivityNarrative(AggregatedEventBundle bundle) {
        StringBuilder narrative = new StringBuilder();
        
        // Employee and session context with ALL available details
        narrative.append("=== EMPLOYEE WORK SESSION ANALYSIS ===\n");
        narrative.append("Employee: ").append(bundle.getEmployeeName() != null ? bundle.getEmployeeName() : bundle.getEmployeeId()).append("\n");
        narrative.append("Employee ID: ").append(bundle.getEmployeeId()).append("\n");
        
        if (bundle.getPcIds() != null && !bundle.getPcIds().isEmpty()) {
            narrative.append("Device(s)/PC ID(s): ").append(String.join(", ", bundle.getPcIds())).append("\n");
        }
        
        if (bundle.getCategories() != null && !bundle.getCategories().isEmpty()) {
            narrative.append("Activity Categories: ").append(String.join(", ", bundle.getCategories())).append("\n");
        }
        
        // Comprehensive Application Usage Analysis
        if (bundle.getAppUsageEvents() != null && !bundle.getAppUsageEvents().isEmpty()) {
            narrative.append("\n=== APPLICATION USAGE DETAILS ===\n");
            narrative.append("Total Applications Used: ").append(bundle.getAppUsageEvents().size()).append("\n");
            
            // Group by app name and include ALL details
            Map<String, List<AppUsageEvent>> appGroups = bundle.getAppUsageEvents().stream()
                .filter(event -> event.getAppName() != null && !event.getAppName().trim().isEmpty())
                .collect(Collectors.groupingBy(event -> event.getAppName().trim()));
            
            appGroups.forEach((appName, events) -> {
                long totalDuration = events.stream().mapToLong(AppUsageEvent::getDurationInSeconds).sum();
                Duration duration = Duration.ofSeconds(totalDuration);
                
                narrative.append("\nApplication: ").append(appName).append("\n");
                narrative.append("  - Total Usage Time: ").append(formatDuration(duration)).append("\n");
                narrative.append("  - Number of Sessions: ").append(events.size()).append("\n");
                
                // Include details from each session
                events.forEach(event -> {
                    narrative.append("  - Session Details:\n");
                    narrative.append("    * Duration: ").append(formatDuration(Duration.ofSeconds(event.getDurationInSeconds()))).append("\n");
                    if (event.getTimestamp() != null) {
                        narrative.append("    * Timestamp: ").append(event.getTimestamp()).append("\n");
                    }
                    if (event.getPcId() != null) {
                        narrative.append("    * Device ID: ").append(event.getPcId()).append("\n");
                    }
                    if (event.getCategory() != null) {
                        narrative.append("    * Category: ").append(event.getCategory()).append("\n");
                    }
                    if (event.getPriority() != null) {
                        narrative.append("    * Priority: ").append(event.getPriority()).append("\n");
                    }
                    if (event.getMetadata() != null && !event.getMetadata().isEmpty()) {
                        narrative.append("    * Metadata: ").append(event.getMetadata()).append("\n");
                    }
                });
            });
        }
        
        // Comprehensive Security Events Analysis
        if (bundle.getSecurityEvents() != null && !bundle.getSecurityEvents().isEmpty()) {
            narrative.append("\n=== SECURITY EVENTS ANALYSIS ===\n");
            narrative.append("Total Security Events: ").append(bundle.getSecurityEvents().size()).append("\n");
            
            bundle.getSecurityEvents().forEach(event -> {
                narrative.append("\nSecurity Event:\n");
                if (event.getEventId() != null) {
                    narrative.append("  - Event ID: ").append(event.getEventId()).append(" (unique identifier for this event)\n");
                }
                if (event.getTimestamp() != null) {
                    narrative.append("  - Timestamp: ").append(event.getTimestamp()).append(" (when the event occurred)\n");
                }
                if (event.getUrl() != null) {
                    narrative.append("  - URL: ").append(event.getUrl()).append(" (the exact website or resource accessed)\n");
                }
                if (event.getThreatType() != null) {
                    narrative.append("  - Threat Type: ").append(event.getThreatType()).append(" (type of security threat detected)\n");
                }
                if (event.getPcId() != null) {
                    narrative.append("  - Device ID: ").append(event.getPcId()).append(" (device where the event occurred)\n");
                }
                if (event.getCategory() != null) {
                    narrative.append("  - Category: ").append(event.getCategory()).append(" (category of the event)\n");
                }
                if (event.getPriority() != null) {
                    narrative.append("  - Priority Level: ").append(event.getPriority()).append(" (priority of the event)\n");
                }
                if (event.getMetadata() != null && !event.getMetadata().isEmpty()) {
                    narrative.append("  - Additional Details:\n");
                    event.getMetadata().forEach((key, value) -> {
                        String label = switch (key) {
                            case "riskScore" -> "Risk Score (risk level assigned by detection engine)";
                            case "geoLocation" -> "Geo Location (location of the threat source)";
                            case "redirectCount" -> "Redirect Count (number of redirects detected)";
                            case "blockReason" -> "Block Reason (reason the event was blocked)";
                            case "detectionEngine" -> "Detection Engine (engine that detected the threat)";
                            default -> key;
                        };
                        narrative.append("    * ").append(label).append(": ").append(value).append("\n");
                    });
                }
            });
        }
        
        // Comprehensive Alerts/Incidents Analysis
        if (bundle.getAlertEvents() != null && !bundle.getAlertEvents().isEmpty()) {
            narrative.append("\n=== ALERTS AND INCIDENTS ANALYSIS ===\n");
            narrative.append("Total Alerts: ").append(bundle.getAlertEvents().size()).append("\n");
            
            // Group by alert type for better analysis
            Map<String, List<AlertEvent>> alertGroups = bundle.getAlertEvents().stream()
                .filter(event -> event.getAlertType() != null && !event.getAlertType().trim().isEmpty())
                .collect(Collectors.groupingBy(event -> event.getAlertType().trim()));
            
            alertGroups.forEach((alertType, events) -> {
                narrative.append("\nAlert Type: ").append(alertType).append("\n");
                narrative.append("  - Count: ").append(events.size()).append("\n");
                
                events.forEach(event -> {
                    narrative.append("  - Alert Details:\n");
                    if (event.getEventId() != null) {
                        narrative.append("    * Event ID: ").append(event.getEventId()).append("\n");
                    }
                    if (event.getTimestamp() != null) {
                        narrative.append("    * Timestamp: ").append(event.getTimestamp()).append("\n");
                    }
                    if (event.getSeverity() != null) {
                        narrative.append("    * Severity: ").append(event.getSeverity()).append("\n");
                    }
                    if (event.getPcId() != null) {
                        narrative.append("    * Device ID: ").append(event.getPcId()).append("\n");
                    }
                    if (event.getCategory() != null) {
                        narrative.append("    * Category: ").append(event.getCategory()).append("\n");
                    }
                    if (event.getPriority() != null) {
                        narrative.append("    * Priority Level: ").append(event.getPriority()).append("\n");
                    }
                    if (event.getMetadata() != null && !event.getMetadata().isEmpty()) {
                        narrative.append("    * Additional Metadata: ").append(event.getMetadata()).append("\n");
                    }
                });
            });
        }
        
        return narrative.toString();
    }

    private String generateComprehensiveFallbackSummary(AggregatedEventBundle bundle) {
        StringBuilder summary = new StringBuilder();
        String employeeName = bundle.getEmployeeName() != null ? 
            bundle.getEmployeeName() : ("Employee " + bundle.getEmployeeId());
        
        // Employee Context
        summary.append("Employee Activity Report: ").append(employeeName)
               .append(" (ID: ").append(bundle.getEmployeeId()).append(")");
        
        // Device Information
        if (!bundle.getPcIds().isEmpty()) {
            summary.append("\n\nWorkstation Information: ");
            if (bundle.getPcIds().size() == 1) {
                summary.append("Activity recorded on device ")
                      .append(bundle.getPcIds().iterator().next());
            } else {
                summary.append("Activity recorded across multiple devices: ")
                      .append(String.join(", ", bundle.getPcIds()));
            }
            summary.append(".");
        }
        
        // App Usage Summary
        summary.append("\n\nApplication Usage: ");
        if (bundle.getAppUsageEvents() != null && !bundle.getAppUsageEvents().isEmpty()) {
            List<AppUsageEvent> validAppEvents = bundle.getAppUsageEvents().stream()
                .filter(event -> event.getAppName() != null && !event.getAppName().trim().isEmpty())
                .toList();
            
            if (validAppEvents.isEmpty()) {
                summary.append("Application usage was recorded but application names are not available.");
            } else {
                Map<String, Long> appDurations = validAppEvents.stream()
                    .collect(Collectors.groupingBy(
                        AppUsageEvent::getAppName,
                        Collectors.summingLong(AppUsageEvent::getDurationInSeconds)
                    ));
                
                summary.append("Used ").append(appDurations.size())
                      .append(appDurations.size() == 1 ? " application" : " applications")
                      .append(" during this session.");
                
                appDurations.forEach((app, duration) -> {
                    summary.append("\n- ").append(app).append(": ")
                          .append(formatDuration(Duration.ofSeconds(duration)));
                });
            }
        } else {
            summary.append("No application activity recorded.");
        }
        
        // Security Events
        if (bundle.getSecurityEvents() != null && !bundle.getSecurityEvents().isEmpty()) {
            bundle.getSecurityEvents().forEach(event -> {
                summary.append("\n\nSecurity Incident Report");
                
                // Always include timestamp and device if available
                if (event.getTimestamp() != null) {
                    summary.append("\nTimestamp: ").append(event.getTimestamp());
                }
                
                if (event.getPcId() != null) {
                    summary.append("\nAffected Device: ").append(event.getPcId());
                }
                
                // Include available security details
                boolean hasSecurityDetails = false;
                
                if (event.getCategory() != null && !event.getCategory().trim().isEmpty()) {
                    summary.append("\nIncident Category: ").append(event.getCategory());
                    hasSecurityDetails = true;
                }
                
                if (event.getThreatType() != null && !event.getThreatType().trim().isEmpty()) {
                    summary.append("\nThreat Classification: ").append(event.getThreatType());
                    hasSecurityDetails = true;
                }
                
                if (event.getUrl() != null && !event.getUrl().trim().isEmpty()) {
                    summary.append("\nTarget URL: ").append(event.getUrl());
                    hasSecurityDetails = true;
                }
                
                if (event.getPriority() != null) {
                    summary.append("\nIncident Priority: ").append(event.getPriority().toString());
                    hasSecurityDetails = true;
                }
                
                if (!hasSecurityDetails) {
                    summary.append("\nNote: Detailed security information is not available for this incident.");
                }
                
                // Include metadata if available
                if (event.getMetadata() != null && !event.getMetadata().isEmpty()) {
                    summary.append("\n\nTechnical Analysis:");
                    event.getMetadata().forEach((key, value) -> {
                        if (value != null && !value.toString().trim().isEmpty()) {
                            switch (key) {
                                case "riskScore" -> summary.append("\n• Threat Risk Score: ")
                                    .append(value).append("/10");
                                case "geoLocation" -> summary.append("\n• Geographic Origin: ")
                                    .append(value);
                                case "redirectCount" -> summary.append("\n• Redirect Chain Length: ")
                                    .append(value);
                                case "blockReason" -> summary.append("\n• Prevention Action: ")
                                    .append("Blocked - ").append(value);
                                case "detectionEngine" -> summary.append("\n• Detection Method: ")
                                    .append(value);
                                default -> {
                                    String formattedKey = key.substring(0, 1).toUpperCase() + 
                                        key.substring(1).replaceAll("([A-Z])", " $1").toLowerCase();
                                    summary.append("\n• ").append(formattedKey).append(": ")
                                        .append(value);
                                }
                            }
                        }
                    });
                }
            });
        }
        
        // Alert Events
        if (bundle.getAlertEvents() != null && !bundle.getAlertEvents().isEmpty()) {
            bundle.getAlertEvents().forEach(event -> {
                summary.append("\n\nSystem Alert");
                
                boolean hasAlertDetails = false;
                
                if (event.getTimestamp() != null) {
                    summary.append("\nTimestamp: ").append(event.getTimestamp());
                }
                
                if (event.getPcId() != null) {
                    summary.append("\nSource Device: ").append(event.getPcId());
                }
                
                if (event.getAlertType() != null && !event.getAlertType().trim().isEmpty()) {
                    summary.append("\nAlert Classification: ").append(event.getAlertType());
                    hasAlertDetails = true;
                }
                
                if (event.getSeverity() != null && !event.getSeverity().trim().isEmpty()) {
                    summary.append("\nSeverity Level: ").append(event.getSeverity());
                    hasAlertDetails = true;
                }
                
                if (event.getPriority() != null) {
                    summary.append("\nPriority Rating: ").append(event.getPriority().toString());
                    hasAlertDetails = true;
                }
                
                if (!hasAlertDetails) {
                    summary.append("\nNote: Detailed alert information is not available.");
                }
            });
        }
        
        return summary.toString().trim();
    }

    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%dm", minutes);
        } else {
            return "< 1m";
        }
    }
} 