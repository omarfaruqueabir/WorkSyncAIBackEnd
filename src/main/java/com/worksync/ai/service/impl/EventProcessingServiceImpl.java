package com.worksync.ai.service.impl;

import com.worksync.ai.model.AppUsageEvent;
import com.worksync.ai.model.SecurityEvent;
import com.worksync.ai.model.AlertEvent;
import com.worksync.ai.repository.AppUsageEventRepository;
import com.worksync.ai.repository.SecurityEventRepository;
import com.worksync.ai.repository.AlertEventRepository;
import com.worksync.ai.service.EventProcessingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class EventProcessingServiceImpl implements EventProcessingService {

    @Autowired
    private AppUsageEventRepository appUsageEventRepository;

    @Autowired
    private SecurityEventRepository securityEventRepository;

    @Autowired
    private AlertEventRepository alertEventRepository;

    @Override
    @Transactional
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void processAppUsageEvent(AppUsageEvent event) {
        try {
            log.debug("Processing AppUsageEvent: {}", event);
            validateEvent(event);
            AppUsageEvent savedEvent = appUsageEventRepository.save(event);
            log.info("Successfully processed and stored AppUsageEvent with ID: {}", savedEvent.getEventId());
        } catch (Exception e) {
            log.error("Error processing AppUsageEvent: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void processSecurityEvent(SecurityEvent event) {
        try {
            log.debug("Processing SecurityEvent: {}", event);
            validateEvent(event);
            SecurityEvent savedEvent = securityEventRepository.save(event);
            log.info("Successfully processed and stored SecurityEvent with ID: {}", savedEvent.getEventId());
        } catch (Exception e) {
            log.error("Error processing SecurityEvent: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void processAlertEvent(AlertEvent event) {
        try {
            log.debug("Processing AlertEvent: {}", event);
            validateEvent(event);
            AlertEvent savedEvent = alertEventRepository.save(event);
            log.info("Successfully processed and stored AlertEvent with ID: {}", savedEvent.getEventId());
        } catch (Exception e) {
            log.error("Error processing AlertEvent: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void validateEvent(Object event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
    }
} 