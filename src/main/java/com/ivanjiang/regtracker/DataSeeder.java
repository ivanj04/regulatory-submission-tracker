package com.ivanjiang.regtracker;

import com.ivanjiang.regtracker.model.StatusHistory;
import com.ivanjiang.regtracker.model.Submission;
import com.ivanjiang.regtracker.model.User;
import com.ivanjiang.regtracker.model.enums.*;
import com.ivanjiang.regtracker.repository.SubmissionRepository;
import com.ivanjiang.regtracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (submissionRepository.count() > 0) {
            log.info("Data already seeded — skipping.");
            return;
        }

        // Seed users
        User admin = User.builder()
                .userId("USR-ADMIN-001")
                .username("admin")
                .email("admin@pharma.example.com")
                .passwordHash(passwordEncoder.encode("Admin@1234"))
                .role(Role.ADMIN)
                .department("Regulatory Affairs")
                .createdAt(Instant.now())
                .build();

        User reviewer = User.builder()
                .userId("USR-REV-001")
                .username("reviewer")
                .email("reviewer@pharma.example.com")
                .passwordHash(passwordEncoder.encode("Review@1234"))
                .role(Role.REVIEWER)
                .department("Global Regulatory Sciences")
                .createdAt(Instant.now())
                .build();

        User submitter = User.builder()
                .userId("USR-SUB-001")
                .username("submitter")
                .email("submitter@pharma.example.com")
                .passwordHash(passwordEncoder.encode("Submit@1234"))
                .role(Role.SUBMITTER)
                .department("Clinical Development")
                .createdAt(Instant.now())
                .build();

        userRepository.saveAll(List.of(admin, reviewer, submitter));

        // Seed submission 1: DRAFT
        Submission draft = Submission.builder()
                .submissionId("IND-2024-001")
                .type(SubmissionType.IND)
                .title("Phase I Clinical Trial – BMS-9876543 Oncology IND")
                .sponsor("Bristol Myers Squibb")
                .targetAuthority(TargetAuthority.FDA)
                .status(SubmissionStatus.DRAFT)
                .assignedTo("USR-SUB-001")
                .dueDate(LocalDate.now().plusDays(45))
                .createdAt(Instant.now().minusSeconds(86400 * 10))
                .updatedAt(Instant.now().minusSeconds(86400 * 10))
                .statusHistory(new ArrayList<>(List.of(
                        StatusHistory.builder()
                                .status(SubmissionStatus.DRAFT)
                                .changedBy("admin")
                                .changedAt(Instant.now().minusSeconds(86400 * 10))
                                .notes("Initial creation of IND application")
                                .build()
                )))
                .build();

        // Seed submission 2: SUBMITTED
        Submission submitted = Submission.builder()
                .submissionId("NDA-2024-001")
                .type(SubmissionType.NDA)
                .title("NDA for Apixelumab – Novel PD-1 Inhibitor – 505(b)(1)")
                .sponsor("Bristol Myers Squibb")
                .targetAuthority(TargetAuthority.FDA)
                .status(SubmissionStatus.SUBMITTED)
                .assignedTo("USR-REV-001")
                .dueDate(LocalDate.now().plusDays(180))
                .submittedAt(Instant.now().minusSeconds(86400 * 5))
                .createdAt(Instant.now().minusSeconds(86400 * 30))
                .updatedAt(Instant.now().minusSeconds(86400 * 5))
                .statusHistory(new ArrayList<>(Arrays.asList(
                        StatusHistory.builder()
                                .status(SubmissionStatus.DRAFT)
                                .changedBy("submitter")
                                .changedAt(Instant.now().minusSeconds(86400 * 30))
                                .notes("Application initiated")
                                .build(),
                        StatusHistory.builder()
                                .status(SubmissionStatus.IN_PREPARATION)
                                .changedBy("submitter")
                                .changedAt(Instant.now().minusSeconds(86400 * 20))
                                .notes("CTD modules compilation started")
                                .build(),
                        StatusHistory.builder()
                                .status(SubmissionStatus.SUBMITTED)
                                .changedBy("admin")
                                .changedAt(Instant.now().minusSeconds(86400 * 5))
                                .notes("Electronic submission to FDA eCTD gateway completed")
                                .build()
                )))
                .build();

        // Seed submission 3: UNDER_REVIEW
        Submission underReview = Submission.builder()
                .submissionId("MAA-2024-001")
                .type(SubmissionType.MAA)
                .title("MAA for Rilvetinib – EGFR Mutation+ NSCLC – EMA Centralised Procedure")
                .sponsor("Bristol Myers Squibb")
                .targetAuthority(TargetAuthority.EMA)
                .status(SubmissionStatus.UNDER_REVIEW)
                .assignedTo("USR-REV-001")
                .dueDate(LocalDate.now().plusDays(90))
                .submittedAt(Instant.now().minusSeconds(86400 * 60))
                .createdAt(Instant.now().minusSeconds(86400 * 90))
                .updatedAt(Instant.now().minusSeconds(86400 * 2))
                .statusHistory(new ArrayList<>(Arrays.asList(
                        StatusHistory.builder()
                                .status(SubmissionStatus.DRAFT)
                                .changedBy("submitter")
                                .changedAt(Instant.now().minusSeconds(86400 * 90))
                                .notes("Application initiated")
                                .build(),
                        StatusHistory.builder()
                                .status(SubmissionStatus.IN_PREPARATION)
                                .changedBy("submitter")
                                .changedAt(Instant.now().minusSeconds(86400 * 80))
                                .notes("Module 5 clinical study reports under compilation")
                                .build(),
                        StatusHistory.builder()
                                .status(SubmissionStatus.SUBMITTED)
                                .changedBy("admin")
                                .changedAt(Instant.now().minusSeconds(86400 * 60))
                                .notes("Dossier submitted via EMA eSubmission gateway")
                                .build(),
                        StatusHistory.builder()
                                .status(SubmissionStatus.UNDER_REVIEW)
                                .changedBy("reviewer")
                                .changedAt(Instant.now().minusSeconds(86400 * 2))
                                .notes("CHMP Day 70 List of Questions received — assessment ongoing")
                                .build()
                )))
                .build();

        submissionRepository.saveAll(List.of(draft, submitted, underReview));
        log.info("Data seeder: 3 users and 3 sample submissions created.");
    }
}
