package com.worksync.ai.entity;

import com.worksync.ai.enums.EventType;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Document(indexName = "event-aggregations")
public class EventAggregation {
    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String employeeId;

    @Field(type = FieldType.Keyword)
    private String employeeName;

    @Field(type = FieldType.Keyword)
    private EventType eventType;

    @Field(type = FieldType.Date)
    private LocalDateTime startTime;

    @Field(type = FieldType.Date)
    private LocalDateTime endTime;

    @Field(type = FieldType.Object)
    private Map<String, Object> aggregatedData;

    @Field(type = FieldType.Text)
    private String summary;

    @Field(type = FieldType.Date)
    private LocalDateTime createdAt;

    @Field(type = FieldType.Date)
    private LocalDateTime updatedAt;
} 