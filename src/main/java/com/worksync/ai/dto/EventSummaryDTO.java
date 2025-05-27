package com.worksync.ai.dto;

import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import java.time.LocalDateTime;

@Document(indexName = "worksync-summaries")
public record EventSummaryDTO(
    @Field(type = FieldType.Keyword)
    String entityId,

    @Field(type = FieldType.Keyword)
    String entityType,

    @Field(type = FieldType.Date)
    LocalDateTime timestamp,

    @Field(type = FieldType.Text)
    String summaryText,

    @Field(type = FieldType.Dense_Vector, dims = 1536)
    float[] embedding
) {
    public EventSummaryDTO {
        if (entityId == null || entityId.isBlank()) {
            throw new IllegalArgumentException("Entity ID cannot be null or blank");
        }
        if (entityType == null || entityType.isBlank()) {
            throw new IllegalArgumentException("Entity type cannot be null or blank");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
        if (summaryText == null || summaryText.isBlank()) {
            throw new IllegalArgumentException("Summary text cannot be null or blank");
        }
        if (embedding == null || embedding.length == 0) {
            throw new IllegalArgumentException("Embedding cannot be null or empty");
        }
    }
} 