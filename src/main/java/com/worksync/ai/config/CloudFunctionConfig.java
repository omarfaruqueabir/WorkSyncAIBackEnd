package com.worksync.ai.config;

import com.worksync.ai.model.AlertEvent;
import com.worksync.ai.model.AppUsageEvent;
import com.worksync.ai.model.SecurityEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import java.util.function.Consumer;
import com.worksync.ai.consumer.EventConsumer;

@Configuration
public class CloudFunctionConfig {

    @Bean("registeredAppUsageConsumer")
    public Consumer<AppUsageEvent> appUsageConsumer(EventConsumer eventConsumer) {
        return eventConsumer.appUsageConsumer();
    }

    @Bean("registeredAlertConsumer")
    public Consumer<AlertEvent> alertConsumer(EventConsumer eventConsumer) {
        return eventConsumer.alertConsumer();
    }

    @Bean("registeredSecurityConsumer")
    public Consumer<SecurityEvent> securityConsumer(EventConsumer eventConsumer) {
        return eventConsumer.securityConsumer();
    }
} 