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
public class AppUsageEvent extends BaseEvent {

    @Field(type = FieldType.Keyword)
    @JsonProperty("appName")
    private String appName;

    @Field(type = FieldType.Integer)
    @JsonProperty("durationInSeconds")
    private long durationInSeconds;
} 