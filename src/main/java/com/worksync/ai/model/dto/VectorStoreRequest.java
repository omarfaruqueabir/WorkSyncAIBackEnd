package com.worksync.ai.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VectorStoreRequest {
    private String employeeId;
    private String summary;
    private LocalDateTime timestamp;
} 