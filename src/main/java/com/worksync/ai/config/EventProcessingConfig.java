package com.worksync.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.Data;
import java.util.concurrent.ConcurrentLinkedQueue;
import com.worksync.ai.model.AppUsageEvent;
import com.worksync.ai.model.SecurityEvent;
import com.worksync.ai.model.AlertEvent;

@Configuration
public class EventProcessingConfig {

    @Bean
    @ConfigurationProperties(prefix = "priority-processing")
    public PriorityProcessingProperties priorityProcessingProperties() {
        return new PriorityProcessingProperties();
    }

    @Bean
    public ConcurrentLinkedQueue<AppUsageEvent> highPriorityAppUsageQueue() {
        return new ConcurrentLinkedQueue<>();
    }

    @Bean
    public ConcurrentLinkedQueue<AppUsageEvent> normalPriorityAppUsageQueue() {
        return new ConcurrentLinkedQueue<>();
    }

    @Bean
    public ConcurrentLinkedQueue<SecurityEvent> highPrioritySecurityQueue() {
        return new ConcurrentLinkedQueue<>();
    }

    @Bean
    public ConcurrentLinkedQueue<SecurityEvent> normalPrioritySecurityQueue() {
        return new ConcurrentLinkedQueue<>();
    }

    @Bean
    public ConcurrentLinkedQueue<AlertEvent> highPriorityAlertQueue() {
        return new ConcurrentLinkedQueue<>();
    }

    @Bean
    public ConcurrentLinkedQueue<AlertEvent> normalPriorityAlertQueue() {
        return new ConcurrentLinkedQueue<>();
    }

    @Data
    public static class PriorityProcessingProperties {
        private long critical;
        private long high;
        private long normal;
    }
} 