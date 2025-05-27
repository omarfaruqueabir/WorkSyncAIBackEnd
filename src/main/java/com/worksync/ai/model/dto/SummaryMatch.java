package com.worksync.ai.model.dto;

import java.time.LocalDateTime;

public record SummaryMatch(
    String employeeId,
    String summary,
    LocalDateTime timestamp,
    double similarity
) {} 