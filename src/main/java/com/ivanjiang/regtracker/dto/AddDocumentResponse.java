package com.ivanjiang.regtracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AddDocumentResponse {
    private String documentId;
    private String presignedUploadUrl;
    private String fileKey;
    private String message;
}
