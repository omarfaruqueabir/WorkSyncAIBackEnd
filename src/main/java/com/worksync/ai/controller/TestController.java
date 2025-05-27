package com.worksync.ai.controller;

import com.worksync.ai.client.OpenRouterClient;
import com.worksync.ai.service.EmbeddingAndVectorStorageService;
import com.worksync.ai.service.ChatbotService;
import com.worksync.ai.model.dto.SummaryMatch;
import com.worksync.ai.model.dto.ChatbotRequest;
import com.worksync.ai.model.dto.ChatbotResponse;
import com.worksync.ai.model.AlertEvent;
import com.worksync.ai.model.AppUsageEvent;
import com.worksync.ai.model.SecurityEvent;
import com.worksync.ai.model.enums.EventType;
import com.worksync.ai.model.enums.Priority;
import com.worksync.ai.consumer.EventConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private OpenRouterClient openRouterClient;

    @Autowired
    private EmbeddingAndVectorStorageService embeddingAndVectorStorageService;

    @Autowired
    private ChatbotService chatbotService;

    @Autowired
    private EventConsumer eventConsumer;

    @GetMapping("/ai")
    public String testAI() {
        try {
            String response = openRouterClient.chatCompletion(
                "You are a helpful assistant.",
                "Hello, this is a test. Please respond with 'Test successful'."
            );
            return "AI Response: " + (response != null ? response : "No response received");
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @GetMapping("/embedding")
    public String testEmbedding() {
        try {
            float[] embedding = openRouterClient.generateEmbedding("This is a test text for embedding");
            return "Embedding generated successfully. Length: " + (embedding != null ? embedding.length : "null");
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @GetMapping("/store-summary")
    public String testStoreSummary() {
        try {
            String testSummary = "Employee EMP001 used Chrome browser for 2 hours today. The employee also accessed Microsoft Teams for 45 minutes for team meetings. No security incidents were detected. Overall performance was normal with standard application usage patterns.";
            
            embeddingAndVectorStorageService.storeSummaryEmbedding("EMP001", testSummary);
            
            return "Test summary stored successfully for employee EMP001";
        } catch (Exception e) {
            return "Error storing test summary: " + e.getMessage();
        }
    }

    @GetMapping("/similarity-test")
    public String testSimilaritySearch() {
        try {
            List<SummaryMatch> matches = embeddingAndVectorStorageService.similaritySearch("Chrome browser usage", 10);
            
            StringBuilder result = new StringBuilder();
            result.append("Found ").append(matches.size()).append(" matches:\n");
            
            for (SummaryMatch match : matches) {
                result.append("Employee: ").append(match.employeeId())
                      .append(", Similarity: ").append(match.similarity())
                      .append(", Text: ").append(match.summary().substring(0, Math.min(100, match.summary().length())))
                      .append("...\n");
            }
            
            return result.toString();
        } catch (Exception e) {
            return "Error testing similarity search: " + e.getMessage();
        }
    }

    @GetMapping("/store-john-doe-summary")
    public String testStoreJohnDoeSummary() {
        try {
            String johnDoeSummary = "Here is a concise summary of the recent work activity for John Doe:\n\n" +
                "**Summary for John Doe (pc123)**\n\n" +
                "* **App Usage:** No notable application usage patterns or upticks in activity.\n" +
                "* **Security Events:** No security concerns or threats detected.\n" +
                "* **Alerts:** 1 **Performance Alert** triggered, indicating a potential issue that requires attention.";

            embeddingAndVectorStorageService.storeSummaryEmbedding("emp123", johnDoeSummary);
            return "John Doe summary stored successfully";
        } catch (Exception e) {
            return "Error storing John Doe summary: " + e.getMessage();
        }
    }

    @GetMapping("/chatbot-query")
    public String testChatbotQuery(@RequestParam(defaultValue = "any performance alerts for John Doe?") String query) {
        try {
            ChatbotRequest request = new ChatbotRequest(query, 5);
            ChatbotResponse response = chatbotService.processQuery(request);
            
            StringBuilder result = new StringBuilder();
            result.append("Query: ").append(query).append("\n");
            result.append("Success: ").append(response.isSuccess()).append("\n");
            result.append("Message: ").append(response.getMessage()).append("\n");
            
            if (response.getMatches() != null && !response.getMatches().isEmpty()) {
                result.append("Matches found: ").append(response.getMatches().size()).append("\n");
                for (SummaryMatch match : response.getMatches()) {
                    result.append("- Employee: ").append(match.employeeId())
                          .append(", Similarity: ").append(String.format("%.3f", match.similarity()))
                          .append("\n");
                }
            } else {
                result.append("No raw matches returned (AI processed response)\n");
            }
            
            return result.toString();
        } catch (Exception e) {
            return "Error testing chatbot query: " + e.getMessage();
        }
    }

    @GetMapping("/ai-query-demo")
    public String testAIQueryProcessing(@RequestParam(defaultValue = "any performance alerts for John Doe?") String query) {
        try {
            // Simulate existing summary data (like what would come from vector search)
            String simulatedSummaryData = """
                Summary 1 (Employee: emp123, Similarity: 0.438):
                Here's a summary of the recent work activity for John Doe:
                
                **Summary:**
                * Device: PC123
                * Category: System Health
                
                **Key Insights:**
                * No security events or alerts were detected.
                * 1 performance alert was triggered, indicating a potential issue with the system or application performance.
                
                **Recommendations:**
                * Investigate the performance alert to resolve any issues that may be impacting productivity.
                * Regularly monitor system health to ensure optimal performance and prevent potential downtime.
                
                Summary 2 (Employee: emp123, Similarity: 0.431):
                Here is a concise summary of the recent work activity for John Doe:
                
                **Summary for John Doe (pc123)**
                * **App Usage:** No notable application usage patterns or upticks in activity.
                * **Security Events:** No security concerns or threats detected.
                * **Alerts:** 1 **Performance Alert** triggered, indicating a potential issue that requires attention.
                """;
            
            // AI Processing System Prompt
            String systemPrompt = """
                You are a helpful AI assistant for employee monitoring. 
                Based on the user's query and the provided summary data, extract and present ONLY the relevant information the user asked for. 
                If the user asks about specific alerts or events, filter and show only those. 
                Be concise and focused. Ask follow-up questions if clarification would be helpful. 
                Format your response clearly and highlight key findings.
                """;
            
            // User Prompt with context
            String userPrompt = String.format("""
                User Query: %s
                
                Available Summary Data:
                %s
                
                Based on the user's query and the summary data above, provide a focused answer that addresses EXACTLY what the user is asking for. 
                If they're asking about specific types of events (like performance alerts, security incidents, etc.), extract and present only that information. 
                Be concise and helpful. If the data doesn't contain what they're looking for, say so clearly and suggest what information is available instead. 
                You can ask follow-up questions to clarify their needs.
                """, query, simulatedSummaryData);
            
            // Get AI response
            String aiResponse = openRouterClient.chatCompletion(systemPrompt, userPrompt);
            
            StringBuilder result = new StringBuilder();
            result.append("=== AI-Powered Query Processing Demo ===\n\n");
            result.append("User Query: ").append(query).append("\n\n");
            result.append("AI-Processed Response:\n");
            result.append("========================\n");
            result.append(aiResponse != null ? aiResponse : "AI processing failed");
            result.append("\n========================\n\n");
            result.append("This demonstrates how the enhanced chatbot filters and processes data based on specific user queries,\n");
            result.append("providing focused answers instead of returning all raw summary data.");
            
            return result.toString();
            
        } catch (Exception e) {
            return "Error in AI query processing demo: " + e.getMessage();
        }
    }

    @GetMapping("/send-alert-event")
    public String sendAlertEvent() {
        try {
            AlertEvent alertEvent = new AlertEvent();
            alertEvent.setEventId("550e8400-e29b-41d4-a716-446655440000");
            alertEvent.setTimestamp(LocalDateTime.now());
            alertEvent.setEmployeeId("emp123");
            alertEvent.setEmployeeName("John Doe");
            alertEvent.setPcId("pc123");
            alertEvent.setEventType(EventType.ALERT);
            alertEvent.setCategory("system_health");
            alertEvent.setPriority(Priority.CRITICAL);
            alertEvent.setDescription("High CPU usage detected");
            alertEvent.setAlertType("PERFORMANCE");
            alertEvent.setSeverity("WARNING");
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("cpuUsage", 95.5);
            metadata.put("memoryUsage", 82.3);
            metadata.put("processName", "chrome.exe");
            alertEvent.setMetadata(metadata);
            
            // Send through the consumer directly for testing
            eventConsumer.alertConsumer().accept(alertEvent);
            
            return "AlertEvent sent successfully: " + alertEvent.getEventId();
        } catch (Exception e) {
            return "Error sending AlertEvent: " + e.getMessage();
        }
    }
    
    @GetMapping("/send-app-usage-event")
    public String sendAppUsageEvent() {
        try {
            AppUsageEvent appUsageEvent = new AppUsageEvent();
            appUsageEvent.setEventId("550e8400-e29b-41d4-a716-446655440001");
            appUsageEvent.setTimestamp(LocalDateTime.now());
            appUsageEvent.setEmployeeId("emp123");
            appUsageEvent.setEmployeeName("John Doe");
            appUsageEvent.setPcId("pc123");
            appUsageEvent.setEventType(EventType.APP_USAGE);
            appUsageEvent.setCategory("productivity");
            appUsageEvent.setPriority(Priority.CRITICAL);
            appUsageEvent.setDescription("Application usage tracked");
            appUsageEvent.setAppName("Chrome");
            appUsageEvent.setDurationInSeconds(3600);
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("windowTitle", "WorkSync Dashboard - Chrome");
            metadata.put("processId", 12345);
            metadata.put("memoryUsage", 156.7);
            appUsageEvent.setMetadata(metadata);
            
            // Send through the consumer directly for testing
            eventConsumer.appUsageConsumer().accept(appUsageEvent);
            
            return "AppUsageEvent sent successfully: " + appUsageEvent.getEventId();
        } catch (Exception e) {
            return "Error sending AppUsageEvent: " + e.getMessage();
        }
    }
    
    @GetMapping("/send-security-event")
    public String sendSecurityEvent() {
        try {
            SecurityEvent securityEvent = new SecurityEvent();
            securityEvent.setEventId("550e8400-e29b-41d4-a716-446655440002");
            securityEvent.setTimestamp(LocalDateTime.now());
            securityEvent.setEmployeeId("emp123");
            securityEvent.setEmployeeName("John Doe");
            securityEvent.setPcId("pc123");
            securityEvent.setEventType(EventType.SECURITY);
            securityEvent.setCategory("web_security");
            securityEvent.setPriority(Priority.CRITICAL);
            securityEvent.setDescription("Suspicious website access detected");
            securityEvent.setUrl("https://suspicious-site.com");
            securityEvent.setThreatType("MALWARE");
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("userAgent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            metadata.put("ipAddress", "192.168.1.100");
            metadata.put("riskScore", 75);
            securityEvent.setMetadata(metadata);
            
            // Send through the consumer directly for testing
            eventConsumer.securityConsumer().accept(securityEvent);
            
            return "SecurityEvent sent successfully: " + securityEvent.getEventId();
        } catch (Exception e) {
            return "Error sending SecurityEvent: " + e.getMessage();
        }
    }
    
    @GetMapping("/test-all-events")
    public String testAllEvents() {
        try {
            sendAlertEvent();
            Thread.sleep(1000);
            sendAppUsageEvent();
            Thread.sleep(1000);
            sendSecurityEvent();
            Thread.sleep(1000);
            
            return "All events sent successfully! Check logs and Elasticsearch for processing results.";
        } catch (Exception e) {
            return "Error sending events: " + e.getMessage();
        }
    }
} 