package com.worksync.ai.repository;

import com.worksync.ai.model.AppUsageEvent;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppUsageEventRepository extends ElasticsearchRepository<AppUsageEvent, String> {
    @Query("{\"bool\": {\"must\": [{\"range\": {\"timestamp\": {\"gte\": \"?0\", \"lte\": \"?1\"}}}]}}")
    List<AppUsageEvent> findByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime);
} 