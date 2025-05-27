package com.worksync.ai.mapper;

import com.worksync.ai.dto.AppUsageAggregationDTO;
import com.worksync.ai.dto.SecurityAggregationDTO;
import com.worksync.ai.dto.AlertAggregationDTO;
import com.worksync.ai.entity.EventAggregation;
import com.worksync.ai.enums.EventType;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.List;

@Component
public class EventAggregationMapper {
    
    @SuppressWarnings("unchecked")
    public SecurityAggregationDTO toSecurityDTO(EventAggregation entity) {
        if (entity == null || entity.getEventType() != EventType.SECURITY) {
            return null;
        }
        
        Map<String, Object> data = entity.getAggregatedData();
        Map<String, Integer> threatCounts = (Map<String, Integer>) data.get("threatCounts");
        int totalThreats = threatCounts.values().stream().mapToInt(Integer::intValue).sum();
        
        return new SecurityAggregationDTO(
            entity.getEmployeeId(),
            entity.getEmployeeName(),
            entity.getCreatedAt(),
            threatCounts,
            totalThreats
        );
    }
    
    @SuppressWarnings("unchecked")
    public AppUsageAggregationDTO toAppUsageDTO(EventAggregation entity) {
        if (entity == null || entity.getEventType() != EventType.APP_USAGE) {
            return null;
        }
        
        Map<String, Object> data = entity.getAggregatedData();
        Map<String, Long> appDurations = (Map<String, Long>) data.get("appDurations");
        long totalDuration = appDurations.values().stream().mapToLong(Long::longValue).sum();
        
        return new AppUsageAggregationDTO(
            entity.getEmployeeId(),
            entity.getEmployeeName(),
            entity.getCreatedAt(),
            appDurations,
            totalDuration
        );
    }
    
    @SuppressWarnings("unchecked")
    public AlertAggregationDTO toAlertDTO(EventAggregation entity) {
        if (entity == null || entity.getEventType() != EventType.ALERT) {
            return null;
        }
        
        Map<String, Object> data = entity.getAggregatedData();
        Map<String, List<String>> alertsByType = (Map<String, List<String>>) data.get("alertsByType");
        int totalAlerts = alertsByType.values().stream().mapToInt(List::size).sum();
        
        return new AlertAggregationDTO(
            entity.getEmployeeId(),
            entity.getEmployeeName(),
            entity.getCreatedAt(),
            alertsByType,
            totalAlerts
        );
    }
} 