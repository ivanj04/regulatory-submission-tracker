package com.ivanjiang.regtracker.service;

import com.ivanjiang.regtracker.dto.AddDocumentRequest;
import com.ivanjiang.regtracker.dto.AddDocumentResponse;
import com.ivanjiang.regtracker.dto.CreateSubmissionRequest;
import com.ivanjiang.regtracker.dto.UpdateStatusRequest;
import com.ivanjiang.regtracker.model.Submission;
import com.ivanjiang.regtracker.model.SubmissionDocument;
import com.ivanjiang.regtracker.model.StatusHistory;
import com.ivanjiang.regtracker.model.User;
import com.ivanjiang.regtracker.model.enums.SubmissionStatus;
import com.ivanjiang.regtracker.model.enums.SubmissionType;
import com.ivanjiang.regtracker.model.enums.TargetAuthority;
import com.ivanjiang.regtracker.repository.SubmissionRepository;
import com.ivanjiang.regtracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final NotificationService notificationService;

    public Submission create(CreateSubmissionRequest request, String createdByUsername) {
        // Generate unique submission ID: TYPE-YEAR-SEQ
        String submissionId = generateSubmissionId(request.getType());

        Submission submission = Submission.builder()
                .submissionId(submissionId)
                .type(request.getType())
                .title(request.getTitle())
                .sponsor(request.getSponsor())
                .targetAuthority(request.getTargetAuthority())
                .status(SubmissionStatus.DRAFT)
                .assignedTo(request.getAssignedTo())
                .dueDate(request.getDueDate())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        StatusHistory initial = StatusHistory.builder()
                .status(SubmissionStatus.DRAFT)
                .changedBy(createdByUsername)
                .changedAt(Instant.now())
                .notes("Submission created")
                .build();
        submission.getStatusHistory().add(initial);

        return submissionRepository.save(submission);
    }

    public Page<Submission> list(SubmissionStatus status, SubmissionType type,
                                  TargetAuthority authority, Pageable pageable) {
        boolean hasStatus = status != null;
        boolean hasType = type != null;
        boolean hasAuthority = authority != null;

        if (hasStatus && hasType && hasAuthority) {
            return submissionRepository.findByStatusAndTypeAndTargetAuthority(status, type, authority, pageable);
        } else if (hasStatus && hasType) {
            return submissionRepository.findByStatusAndType(status, type, pageable);
        } else if (hasStatus && hasAuthority) {
            return submissionRepository.findByStatusAndTargetAuthority(status, authority, pageable);
        } else if (hasType && hasAuthority) {
            return submissionRepository.findByTypeAndTargetAuthority(type, authority, pageable);
        } else if (hasStatus) {
            return submissionRepository.findByStatus(status, pageable);
        } else if (hasType) {
            return submissionRepository.findByType(type, pageable);
        } else if (hasAuthority) {
            return submissionRepository.findByTargetAuthority(authority, pageable);
        } else {
            return submissionRepository.findAll(pageable);
        }
    }

    public Submission getBySubmissionId(String submissionId) {
        return submissionRepository.findBySubmissionId(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found: " + submissionId));
    }

    public Submission updateStatus(String submissionId, UpdateStatusRequest request, String changedByUsername) {
        Submission submission = getBySubmissionId(submissionId);
        SubmissionStatus currentStatus = submission.getStatus();
        SubmissionStatus newStatus = request.getNewStatus();

        if (!currentStatus.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    String.format("Invalid status transition from %s to %s", currentStatus, newStatus));
        }

        submission.setStatus(newStatus);
        submission.setUpdatedAt(Instant.now());

        if (newStatus == SubmissionStatus.SUBMITTED) {
            submission.setSubmittedAt(Instant.now());
        }

        StatusHistory historyEntry = StatusHistory.builder()
                .status(newStatus)
                .changedBy(changedByUsername)
                .changedAt(Instant.now())
                .notes(request.getNotes())
                .build();
        submission.getStatusHistory().add(historyEntry);

        Submission saved = submissionRepository.save(submission);

        // Trigger async notification
        String assigneeEmail = resolveAssigneeEmail(submission.getAssignedTo());
        notificationService.sendStatusUpdate(saved, newStatus, assigneeEmail);

        return saved;
    }

    public AddDocumentResponse addDocument(String submissionId, AddDocumentRequest request, String uploadedByUsername) {
        Submission submission = getBySubmissionId(submissionId);

        String documentId = "DOC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String fileKey = s3Service.buildS3Key(submissionId, documentId, request.getFileName());
        String presignedUrl = s3Service.generatePresignedUploadUrl(fileKey, request.getFileType());

        SubmissionDocument doc = SubmissionDocument.builder()
                .documentId(documentId)
                .fileName(request.getFileName())
                .fileKey(fileKey)
                .fileType(request.getFileType())
                .uploadedBy(uploadedByUsername)
                .uploadedAt(Instant.now())
                .version(request.getVersion() > 0 ? request.getVersion() : 1)
                .description(request.getDescription())
                .deleted(false)
                .build();

        submission.getDocuments().add(doc);
        submission.setUpdatedAt(Instant.now());
        submissionRepository.save(submission);

        return new AddDocumentResponse(documentId, presignedUrl, fileKey,
                "Document registered. Upload your file using the presigned URL.");
    }

    public List<SubmissionDocument> listDocuments(String submissionId) {
        Submission submission = getBySubmissionId(submissionId);
        return submission.getDocuments().stream()
                .filter(d -> !d.isDeleted())
                .toList();
    }

    public void softDeleteDocument(String submissionId, String documentId) {
        Submission submission = getBySubmissionId(submissionId);
        submission.getDocuments().stream()
                .filter(d -> d.getDocumentId().equals(documentId))
                .findFirst()
                .ifPresentOrElse(
                        doc -> doc.setDeleted(true),
                        () -> { throw new IllegalArgumentException("Document not found: " + documentId); }
                );
        submission.setUpdatedAt(Instant.now());
        submissionRepository.save(submission);
    }

    public List<StatusHistory> getHistory(String submissionId) {
        return getBySubmissionId(submissionId).getStatusHistory();
    }

    public String generatePresignedDownloadUrl(String submissionId, String documentId) {
        Submission submission = getBySubmissionId(submissionId);
        return submission.getDocuments().stream()
                .filter(d -> d.getDocumentId().equals(documentId) && !d.isDeleted())
                .findFirst()
                .map(d -> s3Service.generatePresignedDownloadUrl(d.getFileKey()))
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
    }

    private String generateSubmissionId(SubmissionType type) {
        int year = LocalDate.now().getYear();
        long count = submissionRepository.count() + 1;
        String id = String.format("%s-%d-%03d", type.name(), year, count);
        // Ensure uniqueness
        while (submissionRepository.existsBySubmissionId(id)) {
            count++;
            id = String.format("%s-%d-%03d", type.name(), year, count);
        }
        return id;
    }

    private String resolveAssigneeEmail(String userId) {
        if (userId == null) return null;
        return userRepository.findByUserId(userId)
                .map(User::getEmail)
                .orElse(null);
    }
}
