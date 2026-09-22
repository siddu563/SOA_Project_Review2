package com.learnknow.assignment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<AssignmentServiceApplication.Submission, Long> {
    Optional<AssignmentServiceApplication.Submission> findByAssignmentIdAndUserId(Long assignmentId, Long userId);
}
