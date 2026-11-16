package com.ivanjiang.regtracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class ApiError {
    private int status;
    private String error;
    private String message;
    private Instant timestamp;

    public ApiError(int status, String error, String message) {
        this(status, error, message, Instant.now());
    }
}
