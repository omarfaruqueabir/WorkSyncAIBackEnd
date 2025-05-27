package com.worksync.ai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableRetry
public class ApplicationConfig {
    // Configuration beans can be added here if needed
} 