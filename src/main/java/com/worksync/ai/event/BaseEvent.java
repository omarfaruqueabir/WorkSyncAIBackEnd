package com.worksync.ai.event;

import com.worksync.ai.enums.EventType;
import com.worksync.ai.enums.Priority;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Base class for all events in the system.
 * Provides common fields and functionality for all event types.
 */
@Data
@SuperBuilder
@NoArgsConstructor
public abstract class BaseEvent {
    /**
     * Unique identifier for the event
     */
    private String eventId;

    /**
     * Timestamp when the event occurred
     */
    private Instant timestamp;

    /**
     * ID of the employee associated with the event
     */
    private String employeeId;

    /**
     * Name of the employee
     */
    private String employeeName;

    /**
     * ID of the PC/device that generated the event
     */
    private String pcId;

    /**
     * Type of the event (APP_USAGE, SECURITY, ALERT)
     */
    private EventType eventType;

    /**
     * Category of the event for classification
     */
    private String category;

    /**
     * Priority level for processing
     */
    private Priority priority;

    /**
     * Description of the event
     */
    private String description;

    /**
     * Additional metadata associated with the event
     */
    private Map<String, Object> metadata;

    /**
     * Number of times this event has been retried for processing
     */
    private int retryCount;

    /**
     * Default constructor that initializes basic fields
     */
    protected BaseEvent(EventType eventType) {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
        this.eventType = eventType;
        this.retryCount = 0;
    }

    /**
     * Increments the retry count for this event
     */
    public void incrementRetryCount() {
        this.retryCount++;
    }

    /**
     * Checks if the event has exceeded maximum retries
     *
     * @param maxRetries Maximum number of retries allowed
     * @return true if max retries exceeded, false otherwise
     */
    public boolean hasExceededMaxRetries(int maxRetries) {
        return this.retryCount >= maxRetries;
    }
} 