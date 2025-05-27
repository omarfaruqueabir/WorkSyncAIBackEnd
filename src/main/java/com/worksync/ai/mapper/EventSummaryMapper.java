package com.worksync.ai.mapper;

import com.worksync.ai.dto.EventSummaryDTO;
import com.worksync.ai.entity.EventSummary;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class EventSummaryMapper {
    
    public EventSummary toEntity(EventSummaryDTO dto) {
        EventSummary entity = new EventSummary();
        entity.setId(UUID.randomUUID().toString()); // Generate a unique ID
        entity.setEntityId(dto.entityId());
        entity.setEntityType(dto.entityType());
        entity.setTimestamp(dto.timestamp());
        entity.setSummaryText(dto.summaryText());
        entity.setEmbedding(dto.embedding());
        return entity;
    }
    
    public EventSummaryDTO toDto(EventSummary entity) {
        return new EventSummaryDTO(
            entity.getEntityId(),
            entity.getEntityType(),
            entity.getTimestamp(),
            entity.getSummaryText(),
            entity.getEmbedding()
        );
    }
} 