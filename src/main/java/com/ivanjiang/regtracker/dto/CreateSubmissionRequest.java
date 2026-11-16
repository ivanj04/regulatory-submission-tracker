package com.ivanjiang.regtracker.dto;

import com.ivanjiang.regtracker.model.enums.SubmissionType;
import com.ivanjiang.regtracker.model.enums.TargetAuthority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateSubmissionRequest {
    @NotBlank
    private String title;
    @NotNull
    private SubmissionType type;
    @NotBlank
    private String sponsor;
    @NotNull
    private TargetAuthority targetAuthority;
    private String assignedTo;
    private LocalDate dueDate;
}
