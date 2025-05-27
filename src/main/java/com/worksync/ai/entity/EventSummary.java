package com.worksync.ai.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "worksync-summaries")
public class EventSummary {
    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String entityId;

    @Field(type = FieldType.Keyword)
    private String entityType;

    @Field(type = FieldType.Date)
    private LocalDateTime timestamp;

    @Field(type = FieldType.Text)
    private String summaryText;

    @Field(type = FieldType.Dense_Vector, dims = 1536)
    private float[] embedding;
} 