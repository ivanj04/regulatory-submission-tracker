package com.ivanjiang.regtracker.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionDocument {
    private String documentId;
    private String fileName;
    private String fileKey;       // S3 object key
    private String fileType;      // MIME type or category (CTD, Cover Letter, etc.)
    private String uploadedBy;
    private Instant uploadedAt;
    private int version;
    private String description;
    private boolean deleted;      // soft delete flag
}
