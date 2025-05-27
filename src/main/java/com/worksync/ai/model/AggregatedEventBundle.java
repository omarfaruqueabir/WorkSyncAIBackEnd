package com.worksync.ai.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AggregatedEventBundle {
    private String employeeId;
    private String employeeName;
    private Set<String> pcIds;  // Multiple PCs might be used by the same employee
    private Set<String> categories; // Different event categories
    private List<AppUsageEvent> appUsageEvents;
    private List<SecurityEvent> securityEvents;
    private List<AlertEvent> alertEvents;
    
    // Constructor that initializes collections
    public AggregatedEventBundle(String employeeId, String employeeName) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.pcIds = new HashSet<>();
        this.categories = new HashSet<>();
        this.appUsageEvents = new ArrayList<>();
        this.securityEvents = new ArrayList<>();
        this.alertEvents = new ArrayList<>();
    }
}