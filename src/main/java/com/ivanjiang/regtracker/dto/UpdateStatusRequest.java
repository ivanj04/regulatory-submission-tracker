package com.ivanjiang.regtracker.dto;

import com.ivanjiang.regtracker.model.enums.SubmissionStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStatusRequest {
    @NotNull
    private SubmissionStatus newStatus;
    private String notes;
}
