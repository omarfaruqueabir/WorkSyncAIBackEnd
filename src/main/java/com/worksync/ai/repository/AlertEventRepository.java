package com.worksync.ai.repository;

import com.worksync.ai.model.AlertEvent;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertEventRepository extends ElasticsearchRepository<AlertEvent, String> {
    @Query("{\"bool\": {\"must\": [{\"range\": {\"timestamp\": {\"gte\": \"?0\", \"lte\": \"?1\"}}}]}}")
    List<AlertEvent> findByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime);
} 