package com.ivanjiang.regtracker.service;

import com.ivanjiang.regtracker.model.Submission;
import com.ivanjiang.regtracker.model.enums.SubmissionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Async("taskExecutor")
    public void sendStatusUpdate(Submission submission, SubmissionStatus newStatus, String assignedUserEmail) {
        if (!StringUtils.hasText(mailHost)) {
            log.info("[NOTIFICATION - no mail server configured] Submission '{}' ({}) transitioned to {}. " +
                            "Assignee: {}",
                    submission.getSubmissionId(),
                    submission.getTitle(),
                    newStatus,
                    assignedUserEmail != null ? assignedUserEmail : "unassigned");
            return;
        }

        if (!StringUtils.hasText(assignedUserEmail)) {
            log.debug("No assignee email for submission {} — skipping email notification", submission.getSubmissionId());
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(assignedUserEmail);
            message.setSubject(String.format("[RegTracker] Submission %s status updated: %s",
                    submission.getSubmissionId(), newStatus));
            message.setText(buildEmailBody(submission, newStatus));
            mailSender.send(message);
            log.info("Status notification sent to {} for submission {}", assignedUserEmail, submission.getSubmissionId());
        } catch (MailException e) {
            log.error("Failed to send status notification email to {} for submission {}: {}",
                    assignedUserEmail, submission.getSubmissionId(), e.getMessage());
        }
    }

    private String buildEmailBody(Submission submission, SubmissionStatus newStatus) {
        return String.format("""
                Regulatory Submission Status Update
                =====================================

                Submission ID : %s
                Title         : %s
                Type          : %s
                Authority     : %s
                New Status    : %s
                Due Date      : %s

                Please log in to the Regulatory Submission Tracker to review this update.

                This is an automated notification. Do not reply to this email.
                """,
                submission.getSubmissionId(),
                submission.getTitle(),
                submission.getType(),
                submission.getTargetAuthority(),
                newStatus,
                submission.getDueDate() != null ? submission.getDueDate().toString() : "N/A");
    }
}
