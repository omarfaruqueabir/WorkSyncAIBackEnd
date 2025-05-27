package com.worksync.ai.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.worksync.ai.model.enums.SummaryType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "worksync-summaries")
public class EventSummary {

    @Id
    @JsonProperty("entityId")
    private String entityId;

    @Field(type = FieldType.Keyword)
    @JsonProperty("type")
    private SummaryType type;

    @Field(type = FieldType.Date)
    @JsonProperty("timestamp")
    private Instant timestamp;

    @Field(type = FieldType.Text)
    @JsonProperty("summaryText")
    private String summaryText;
} 