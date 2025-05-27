package com.worksync.ai.repository;

import com.worksync.ai.entity.EventAggregation;
import com.worksync.ai.enums.EventType;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventAggregationRepository extends ElasticsearchRepository<EventAggregation, String> {
    
    List<EventAggregation> findByEmployeeIdAndEventTypeAndStartTimeBetween(
        String employeeId, 
        EventType eventType, 
        LocalDateTime startTime, 
        LocalDateTime endTime
    );

    List<EventAggregation> findByEmployeeIdAndStartTimeBetween(
        String employeeId,
        LocalDateTime startTime,
        LocalDateTime endTime
    );

    List<EventAggregation> findByEventTypeAndStartTimeBetween(
        EventType eventType,
        LocalDateTime startTime,
        LocalDateTime endTime
    );
} 