package com.worksync.ai.repository;

import com.worksync.ai.entity.EventSummary;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventSummaryRepository extends ElasticsearchRepository<EventSummary, String> {
    List<EventSummary> findByEntityIdAndTimestampBetween(String entityId, LocalDateTime startTime, LocalDateTime endTime);
} 