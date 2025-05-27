package com.worksync.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WorkSyncApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkSyncApplication.class, args);
    }
} 