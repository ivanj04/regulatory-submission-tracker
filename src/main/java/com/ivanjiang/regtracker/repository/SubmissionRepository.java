package com.ivanjiang.regtracker.repository;

import com.ivanjiang.regtracker.model.Submission;
import com.ivanjiang.regtracker.model.enums.SubmissionStatus;
import com.ivanjiang.regtracker.model.enums.SubmissionType;
import com.ivanjiang.regtracker.model.enums.TargetAuthority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends MongoRepository<Submission, String> {

    Optional<Submission> findBySubmissionId(String submissionId);

    boolean existsBySubmissionId(String submissionId);

    Page<Submission> findByStatus(SubmissionStatus status, Pageable pageable);

    Page<Submission> findByType(SubmissionType type, Pageable pageable);

    Page<Submission> findByTargetAuthority(TargetAuthority authority, Pageable pageable);

    Page<Submission> findByStatusAndType(SubmissionStatus status, SubmissionType type, Pageable pageable);

    Page<Submission> findByStatusAndTargetAuthority(SubmissionStatus status, TargetAuthority authority, Pageable pageable);

    Page<Submission> findByTypeAndTargetAuthority(SubmissionType type, TargetAuthority authority, Pageable pageable);

    Page<Submission> findByStatusAndTypeAndTargetAuthority(
            SubmissionStatus status, SubmissionType type, TargetAuthority authority, Pageable pageable);

    List<Submission> findByDueDateBetween(LocalDate from, LocalDate to);

    @Query("{'status': {$nin: ['APPROVED','REJECTED','WITHDRAWN']}, 'dueDate': {$lte: ?0}}")
    List<Submission> findUpcomingDeadlines(LocalDate cutoff);

    long countByStatus(SubmissionStatus status);

    long countByType(SubmissionType type);

    long countByTargetAuthority(TargetAuthority authority);
}
