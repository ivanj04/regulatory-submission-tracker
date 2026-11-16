package com.ivanjiang.regtracker;

import com.ivanjiang.regtracker.dto.CreateSubmissionRequest;
import com.ivanjiang.regtracker.dto.UpdateStatusRequest;
import com.ivanjiang.regtracker.model.Submission;
import com.ivanjiang.regtracker.model.enums.SubmissionStatus;
import com.ivanjiang.regtracker.model.enums.SubmissionType;
import com.ivanjiang.regtracker.model.enums.TargetAuthority;
import com.ivanjiang.regtracker.repository.SubmissionRepository;
import com.ivanjiang.regtracker.repository.UserRepository;
import com.ivanjiang.regtracker.service.NotificationService;
import com.ivanjiang.regtracker.service.S3Service;
import com.ivanjiang.regtracker.service.SubmissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private S3Service s3Service;
    @Mock
    private NotificationService notificationService;

    private SubmissionService submissionService;

    @BeforeEach
    void setUp() {
        submissionService = new SubmissionService(
                submissionRepository, userRepository, s3Service, notificationService);
    }

    @Test
    void create_shouldReturnDraftSubmission() {
        CreateSubmissionRequest req = new CreateSubmissionRequest();
        req.setTitle("Test NDA");
        req.setType(SubmissionType.NDA);
        req.setSponsor("TestCo");
        req.setTargetAuthority(TargetAuthority.FDA);

        when(submissionRepository.count()).thenReturn(0L);
        when(submissionRepository.existsBySubmissionId(any())).thenReturn(false);
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> inv.getArgument(0));

        Submission result = submissionService.create(req, "admin");

        assertThat(result.getStatus()).isEqualTo(SubmissionStatus.DRAFT);
        assertThat(result.getTitle()).isEqualTo("Test NDA");
        assertThat(result.getStatusHistory()).hasSize(1);
        assertThat(result.getStatusHistory().get(0).getStatus()).isEqualTo(SubmissionStatus.DRAFT);
    }

    @Test
    void updateStatus_draftToInPreparation_shouldSucceed() {
        Submission submission = buildSubmission("NDA-2024-001", SubmissionStatus.DRAFT);
        when(submissionRepository.findBySubmissionId("NDA-2024-001")).thenReturn(Optional.of(submission));
        when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateStatusRequest req = new UpdateStatusRequest();
        req.setNewStatus(SubmissionStatus.IN_PREPARATION);
        req.setNotes("Starting document preparation");

        Submission updated = submissionService.updateStatus("NDA-2024-001", req, "admin");

        assertThat(updated.getStatus()).isEqualTo(SubmissionStatus.IN_PREPARATION);
        assertThat(updated.getStatusHistory()).hasSize(2);
        verify(notificationService).sendStatusUpdate(any(), eq(SubmissionStatus.IN_PREPARATION), any());
    }

    @Test
    void updateStatus_fullPipeline_draftToApproved() {
        Submission submission = buildSubmission("NDA-2024-002", SubmissionStatus.DRAFT);
        when(submissionRepository.findBySubmissionId("NDA-2024-002")).thenReturn(Optional.of(submission));
        when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // DRAFT -> IN_PREPARATION
        UpdateStatusRequest req = new UpdateStatusRequest();
        req.setNewStatus(SubmissionStatus.IN_PREPARATION);
        submission = submissionService.updateStatus("NDA-2024-002", req, "submitter");
        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.IN_PREPARATION);

        // IN_PREPARATION -> SUBMITTED
        when(submissionRepository.findBySubmissionId("NDA-2024-002")).thenReturn(Optional.of(submission));
        req.setNewStatus(SubmissionStatus.SUBMITTED);
        submission = submissionService.updateStatus("NDA-2024-002", req, "submitter");
        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.SUBMITTED);
        assertThat(submission.getSubmittedAt()).isNotNull();

        // SUBMITTED -> UNDER_REVIEW
        when(submissionRepository.findBySubmissionId("NDA-2024-002")).thenReturn(Optional.of(submission));
        req.setNewStatus(SubmissionStatus.UNDER_REVIEW);
        submission = submissionService.updateStatus("NDA-2024-002", req, "reviewer");

        // UNDER_REVIEW -> APPROVED
        when(submissionRepository.findBySubmissionId("NDA-2024-002")).thenReturn(Optional.of(submission));
        req.setNewStatus(SubmissionStatus.APPROVED);
        submission = submissionService.updateStatus("NDA-2024-002", req, "reviewer");
        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.APPROVED);
        assertThat(submission.getStatusHistory()).hasSize(5);
    }

    @Test
    void updateStatus_invalidTransition_shouldThrow() {
        Submission submission = buildSubmission("NDA-2024-003", SubmissionStatus.APPROVED);
        when(submissionRepository.findBySubmissionId("NDA-2024-003")).thenReturn(Optional.of(submission));

        UpdateStatusRequest req = new UpdateStatusRequest();
        req.setNewStatus(SubmissionStatus.DRAFT);

        assertThatThrownBy(() -> submissionService.updateStatus("NDA-2024-003", req, "admin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    void updateStatus_approvedToRejected_shouldThrow() {
        Submission submission = buildSubmission("NDA-2024-004", SubmissionStatus.APPROVED);
        when(submissionRepository.findBySubmissionId("NDA-2024-004")).thenReturn(Optional.of(submission));

        UpdateStatusRequest req = new UpdateStatusRequest();
        req.setNewStatus(SubmissionStatus.REJECTED);

        assertThatThrownBy(() -> submissionService.updateStatus("NDA-2024-004", req, "admin"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void updateStatus_notFound_shouldThrow() {
        when(submissionRepository.findBySubmissionId("NONEXISTENT")).thenReturn(Optional.empty());

        UpdateStatusRequest req = new UpdateStatusRequest();
        req.setNewStatus(SubmissionStatus.IN_PREPARATION);

        assertThatThrownBy(() -> submissionService.updateStatus("NONEXISTENT", req, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void submissionStatus_canTransitionTo_correctRules() {
        assertThat(SubmissionStatus.DRAFT.canTransitionTo(SubmissionStatus.IN_PREPARATION)).isTrue();
        assertThat(SubmissionStatus.DRAFT.canTransitionTo(SubmissionStatus.APPROVED)).isFalse();
        assertThat(SubmissionStatus.SUBMITTED.canTransitionTo(SubmissionStatus.UNDER_REVIEW)).isTrue();
        assertThat(SubmissionStatus.UNDER_REVIEW.canTransitionTo(SubmissionStatus.APPROVED)).isTrue();
        assertThat(SubmissionStatus.UNDER_REVIEW.canTransitionTo(SubmissionStatus.REJECTED)).isTrue();
        assertThat(SubmissionStatus.APPROVED.canTransitionTo(SubmissionStatus.DRAFT)).isFalse();
        assertThat(SubmissionStatus.REJECTED.canTransitionTo(SubmissionStatus.SUBMITTED)).isFalse();
        assertThat(SubmissionStatus.ADDITIONAL_INFO_REQUESTED.canTransitionTo(SubmissionStatus.SUBMITTED)).isTrue();
    }

    private Submission buildSubmission(String submissionId, SubmissionStatus status) {
        Submission s = Submission.builder()
                .submissionId(submissionId)
                .type(SubmissionType.NDA)
                .title("Test Submission")
                .sponsor("TestCo")
                .targetAuthority(TargetAuthority.FDA)
                .status(status)
                .build();
        // Add an initial history entry
        com.ivanjiang.regtracker.model.StatusHistory initial =
                com.ivanjiang.regtracker.model.StatusHistory.builder()
                        .status(status)
                        .changedBy("system")
                        .changedAt(java.time.Instant.now())
                        .notes("Initial")
                        .build();
        s.getStatusHistory().add(initial);
        return s;
    }
}
