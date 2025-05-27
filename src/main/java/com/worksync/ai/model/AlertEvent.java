package com.worksync.ai.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(indexName = "worksync-events")
public class AlertEvent extends BaseEvent {

    @Field(type = FieldType.Keyword)
    @JsonProperty("alertType")
    private String alertType;

    @Field(type = FieldType.Keyword)
    @JsonProperty("severity")
    private String severity;
} 