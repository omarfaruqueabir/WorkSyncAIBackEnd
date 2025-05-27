package com.worksync.ai.repository;

import com.worksync.ai.model.SecurityEvent;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SecurityEventRepository extends ElasticsearchRepository<SecurityEvent, String> {
    @Query("{\"bool\": {\"must\": [{\"range\": {\"timestamp\": {\"gte\": \"?0\", \"lte\": \"?1\"}}}]}}")
    List<SecurityEvent> findByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime);
} 