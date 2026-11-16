package com.ivanjiang.regtracker.controller;

import com.ivanjiang.regtracker.dto.AddDocumentRequest;
import com.ivanjiang.regtracker.dto.AddDocumentResponse;
import com.ivanjiang.regtracker.dto.CreateSubmissionRequest;
import com.ivanjiang.regtracker.dto.UpdateStatusRequest;
import com.ivanjiang.regtracker.model.Submission;
import com.ivanjiang.regtracker.model.SubmissionDocument;
import com.ivanjiang.regtracker.model.StatusHistory;
import com.ivanjiang.regtracker.model.enums.SubmissionStatus;
import com.ivanjiang.regtracker.model.enums.SubmissionType;
import com.ivanjiang.regtracker.model.enums.TargetAuthority;
import com.ivanjiang.regtracker.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
@Tag(name = "Submissions", description = "Manage regulatory submissions")
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping
    @Operation(summary = "Create a new submission")
    public ResponseEntity<Submission> create(
            @Valid @RequestBody CreateSubmissionRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(submissionService.create(request, auth.getName()));
    }

    @GetMapping
    @Operation(summary = "List submissions with optional filters")
    public ResponseEntity<Page<Submission>> list(
            @RequestParam(required = false) SubmissionStatus status,
            @RequestParam(required = false) SubmissionType type,
            @RequestParam(required = false) TargetAuthority authority,
            @PageableDefault(size = 20, sort = "updatedAt") Pageable pageable) {
        return ResponseEntity.ok(submissionService.list(status, type, authority, pageable));
    }

    @GetMapping("/{submissionId}")
    @Operation(summary = "Get a submission by its ID")
    public ResponseEntity<Submission> getOne(@PathVariable String submissionId) {
        return ResponseEntity.ok(submissionService.getBySubmissionId(submissionId));
    }

    @PutMapping("/{submissionId}/status")
    @Operation(summary = "Advance submission status (validates transitions)")
    public ResponseEntity<Submission> updateStatus(
            @PathVariable String submissionId,
            @Valid @RequestBody UpdateStatusRequest request,
            Authentication auth) {
        return ResponseEntity.ok(submissionService.updateStatus(submissionId, request, auth.getName()));
    }

    @PostMapping("/{submissionId}/documents")
    @Operation(summary = "Register document metadata and get S3 presigned upload URL")
    public ResponseEntity<AddDocumentResponse> addDocument(
            @PathVariable String submissionId,
            @Valid @RequestBody AddDocumentRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(submissionService.addDocument(submissionId, request, auth.getName()));
    }

    @GetMapping("/{submissionId}/documents")
    @Operation(summary = "List documents for a submission")
    public ResponseEntity<List<SubmissionDocument>> listDocuments(@PathVariable String submissionId) {
        return ResponseEntity.ok(submissionService.listDocuments(submissionId));
    }

    @DeleteMapping("/{submissionId}/documents/{documentId}")
    @Operation(summary = "Soft-delete a document")
    public ResponseEntity<Map<String, String>> deleteDocument(
            @PathVariable String submissionId,
            @PathVariable String documentId) {
        submissionService.softDeleteDocument(submissionId, documentId);
        return ResponseEntity.ok(Map.of("message", "Document deleted", "documentId", documentId));
    }

    @GetMapping("/{submissionId}/documents/{documentId}/download-url")
    @Operation(summary = "Get a presigned download URL for a document")
    public ResponseEntity<Map<String, String>> getDownloadUrl(
            @PathVariable String submissionId,
            @PathVariable String documentId) {
        String url = submissionService.generatePresignedDownloadUrl(submissionId, documentId);
        return ResponseEntity.ok(Map.of("downloadUrl", url, "documentId", documentId));
    }

    @GetMapping("/{submissionId}/history")
    @Operation(summary = "Get status change history for a submission")
    public ResponseEntity<List<StatusHistory>> getHistory(@PathVariable String submissionId) {
        return ResponseEntity.ok(submissionService.getHistory(submissionId));
    }
}
