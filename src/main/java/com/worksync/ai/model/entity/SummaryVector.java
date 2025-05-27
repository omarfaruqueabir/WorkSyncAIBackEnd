package com.worksync.ai.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "worksync-summary-vectors")
public class SummaryVector {
    
    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String employeeId;

    @Field(type = FieldType.Text)
    private String summaryText;

    @Field(type = FieldType.Dense_Vector, dims = 1536)
    private float[] embedding;

    @Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSSSS||uuuu-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
} 