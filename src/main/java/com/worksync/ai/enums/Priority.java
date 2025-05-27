package com.worksync.ai.enums;

/**
 * Enum representing different priority levels for event processing.
 */
public enum Priority {
    /**
     * Critical priority - processed immediately
     */
    CRITICAL,

    /**
     * High priority - processed every 5 minutes
     */
    HIGH,

    /**
     * Normal priority - processed every 30 minutes
     */
    NORMAL
} 