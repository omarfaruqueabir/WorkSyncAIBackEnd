package com.worksync.ai.consumer;

import com.worksync.ai.config.EventProcessingConfig.PriorityProcessingProperties;
import com.worksync.ai.model.AppUsageEvent;
import com.worksync.ai.model.SecurityEvent;
import com.worksync.ai.model.AlertEvent;
import com.worksync.ai.model.enums.Priority;
import com.worksync.ai.service.EventProcessingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

@Slf4j
@Component
public class EventConsumer {

    @Autowired
    private EventProcessingService eventProcessingService;

    @Autowired
    private PriorityProcessingProperties processingProperties;

    @Autowired
    private ConcurrentLinkedQueue<AppUsageEvent> highPriorityAppUsageQueue;

    @Autowired
    private ConcurrentLinkedQueue<AppUsageEvent> normalPriorityAppUsageQueue;

    @Autowired
    private ConcurrentLinkedQueue<SecurityEvent> highPrioritySecurityQueue;

    @Autowired
    private ConcurrentLinkedQueue<SecurityEvent> normalPrioritySecurityQueue;

    @Autowired
    private ConcurrentLinkedQueue<AlertEvent> highPriorityAlertQueue;

    @Autowired
    private ConcurrentLinkedQueue<AlertEvent> normalPriorityAlertQueue;

    @Bean
    public Consumer<AppUsageEvent> appUsageConsumer() {
        return event -> {
            try {
                log.debug("Received AppUsageEvent: {}", event);

                if (event.getPriority() == Priority.CRITICAL) {
                    eventProcessingService.processAppUsageEvent(event);
                } else if (event.getPriority() == Priority.HIGH) {
                    highPriorityAppUsageQueue.offer(event);
                } else {
                    normalPriorityAppUsageQueue.offer(event);
                }
            } catch (Exception e) {
                log.error("Error processing AppUsageEvent: {}", e.getMessage(), e);
                throw e;
            }
        };
    }

    @Bean
    public Consumer<SecurityEvent> securityConsumer() {
        return event -> {
            try {
                log.debug("Received SecurityEvent: {}", event);

                if (event.getPriority() == Priority.CRITICAL) {
                    eventProcessingService.processSecurityEvent(event);
                } else if (event.getPriority() == Priority.HIGH) {
                    highPrioritySecurityQueue.offer(event);
                } else {
                    normalPrioritySecurityQueue.offer(event);
                }
            } catch (Exception e) {
                log.error("Error processing SecurityEvent: {}", e.getMessage(), e);
                throw e;
            }
        };
    }

    @Bean
    public Consumer<AlertEvent> alertConsumer() {
        return event -> {
            try {
                log.debug("Received AlertEvent: {}", event);

                if (event.getPriority() == Priority.CRITICAL) {
                    eventProcessingService.processAlertEvent(event);
                } else if (event.getPriority() == Priority.HIGH) {
                    highPriorityAlertQueue.offer(event);
                } else {
                    normalPriorityAlertQueue.offer(event);
                }
            } catch (Exception e) {
                log.error("Error processing AlertEvent: {}", e.getMessage(), e);
                throw e;
            }
        };
    }

    @Scheduled(fixedRateString = "${priority-processing.high}")
    public void processHighPriorityQueues() {
        processQueue(highPriorityAppUsageQueue, eventProcessingService::processAppUsageEvent, "AppUsageEvent");
        processQueue(highPrioritySecurityQueue, eventProcessingService::processSecurityEvent, "SecurityEvent");
        processQueue(highPriorityAlertQueue, eventProcessingService::processAlertEvent, "AlertEvent");
    }

    @Scheduled(fixedRateString = "${priority-processing.normal}")
    public void processNormalPriorityQueues() {
        processQueue(normalPriorityAppUsageQueue, eventProcessingService::processAppUsageEvent, "AppUsageEvent");
        processQueue(normalPrioritySecurityQueue, eventProcessingService::processSecurityEvent, "SecurityEvent");
        processQueue(normalPriorityAlertQueue, eventProcessingService::processAlertEvent, "AlertEvent");
    }

    private <T> void processQueue(ConcurrentLinkedQueue<T> queue, Consumer<T> processor, String eventType) {
        try {
            T event;
            while ((event = queue.poll()) != null) {
                try {
                    processor.accept(event);
                } catch (Exception e) {
                    log.error("Error processing {} from queue: {}", eventType, e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error processing {} queue: {}", eventType, e.getMessage(), e);
        }
    }
} 