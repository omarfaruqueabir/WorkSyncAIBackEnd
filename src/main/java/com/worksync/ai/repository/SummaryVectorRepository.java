package com.worksync.ai.repository;

import com.worksync.ai.model.entity.SummaryVector;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SummaryVectorRepository extends ElasticsearchRepository<SummaryVector, String> {
} 