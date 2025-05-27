package com.worksync.ai.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.worksync.ai.model.enums.EventType;
import com.worksync.ai.model.enums.Priority;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "worksync-events")
public class BaseEvent {
    
    @Id
    @JsonProperty("eventId")
    private String eventId;

    @Field(type = FieldType.Date)
    @JsonProperty("timestamp")
    private Instant timestamp;

    @Field(type = FieldType.Keyword)
    @JsonProperty("employeeId")
    private String employeeId;

    @Field(type = FieldType.Text)
    @JsonProperty("employeeName")
    private String employeeName;

    @Field(type = FieldType.Keyword)
    @JsonProperty("pcId")
    private String pcId;

    @Field(type = FieldType.Keyword)
    @JsonProperty("eventType")
    private EventType eventType;

    @Field(type = FieldType.Keyword)
    @JsonProperty("category")
    private String category;

    @Field(type = FieldType.Keyword)
    @JsonProperty("priority")
    private Priority priority;

    @Field(type = FieldType.Text)
    @JsonProperty("description")
    private String description;

    @Field(type = FieldType.Object)
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
} 