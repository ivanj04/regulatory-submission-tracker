package com.ivanjiang.regtracker.model;

import com.ivanjiang.regtracker.model.enums.SubmissionStatus;
import com.ivanjiang.regtracker.model.enums.SubmissionType;
import com.ivanjiang.regtracker.model.enums.TargetAuthority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "submissions")
public class Submission {

    @Id
    private String id;

    @Indexed(unique = true)
    private String submissionId;  // e.g. "NDA-2024-001"

    private SubmissionType type;
    private String title;
    private String sponsor;
    private TargetAuthority targetAuthority;
    private SubmissionStatus status;
    private String assignedTo;    // userId
    private LocalDate dueDate;
    private Instant submittedAt;
    private Instant createdAt;
    private Instant updatedAt;

    @Builder.Default
    private List<SubmissionDocument> documents = new ArrayList<>();

    @Builder.Default
    private List<StatusHistory> statusHistory = new ArrayList<>();
}
