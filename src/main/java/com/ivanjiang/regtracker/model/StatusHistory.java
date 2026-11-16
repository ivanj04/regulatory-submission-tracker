package com.ivanjiang.regtracker.model;

import com.ivanjiang.regtracker.model.enums.SubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusHistory {
    private SubmissionStatus status;
    private String changedBy;
    private Instant changedAt;
    private String notes;
}
