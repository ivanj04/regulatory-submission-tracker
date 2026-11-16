package com.ivanjiang.regtracker.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddDocumentRequest {
    @NotBlank
    private String fileName;
    @NotBlank
    private String fileType;      // content-type or regulatory category
    private String description;
    private int version;
}
