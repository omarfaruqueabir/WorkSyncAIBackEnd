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
public class SecurityEvent extends BaseEvent {

    @Field(type = FieldType.Keyword)
    @JsonProperty("url")
    private String url;

    @Field(type = FieldType.Keyword)
    @JsonProperty("threatType")
    private String threatType;
} 