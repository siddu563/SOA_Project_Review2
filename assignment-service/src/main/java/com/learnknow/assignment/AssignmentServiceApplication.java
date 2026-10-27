package com.learnknow.assignment;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@SpringBootApplication
public class AssignmentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssignmentServiceApplication.class, args);
    }

    @Entity
    @Table(name = "assignments")
    public static class Assignment {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        public Long id;

        @Column(nullable = false)
        public Long courseId;

        @Column(nullable = false)
        public String title;

        public String description;

        public LocalDateTime dueDate;
    }

    @Entity
    @Table(name = "submissions")
    public static class Submission {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        public Long id;

        @Column(nullable = false)
        public Long assignmentId;

        @Column(nullable = false)
        public Long userId;

        @Column(columnDefinition = "TEXT", nullable = false)
        public String code;

        public String status = "SUBMITTED";

        public Instant submittedAt = Instant.now();
    }

    public record AssignmentRequest(
            @NotNull(message = "courseId must not be null") Long courseId,
            @NotBlank(message = "title must not be blank") String title,
            String description,
            LocalDateTime dueDate
    ) {}

    public record SubmitRequest(
            @NotNull(message = "userId must not be null") Long userId,
            @NotBlank(message = "code must not be blank") String code
    ) {}

    @RestController
    @RequestMapping("/api/assignments")
    public static class AssignmentController {
        private final AssignmentRepository assignmentRepository;
        private final SubmissionRepository submissionRepository;

        public AssignmentController(AssignmentRepository assignmentRepository, SubmissionRepository submissionRepository) {
            this.assignmentRepository = assignmentRepository;
            this.submissionRepository = submissionRepository;
        }

        @GetMapping("/course/{courseId}")
        public List<Assignment> getByCourse(@PathVariable("courseId") Long courseId) {
            return assignmentRepository.findByCourseId(courseId);
        }

        @GetMapping("/{id}")
        public Assignment getById(@PathVariable("id") Long id) {
            return assignmentRepository.findById(id)
                    .orElseThrow(() -> new NoSuchElementException("Assignment not found with id: " + id));
        }

        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        public Assignment create(@Valid @RequestBody AssignmentRequest r) {
            Assignment a = new Assignment();
            a.courseId = r.courseId();
            a.title = r.title().trim();
            a.description = r.description();
            a.dueDate = r.dueDate() != null ? r.dueDate() : LocalDateTime.now().plusWeeks(1);
            return assignmentRepository.save(a);
        }

        @PostMapping("/{assignmentId}/submit")
        public Submission submit(
                @PathVariable("assignmentId") Long assignmentId,
                @Valid @RequestBody SubmitRequest r) {
            getById(assignmentId); // ensure assignment exists
            Submission s = submissionRepository.findByAssignmentIdAndUserId(assignmentId, r.userId())
                    .orElseGet(Submission::new);
            s.assignmentId = assignmentId;
            s.userId = r.userId();
            s.code = r.code().trim();
            s.status = "SUBMITTED";
            s.submittedAt = Instant.now();
            return submissionRepository.save(s);
        }

        @GetMapping("/{assignmentId}/submissions/{userId}")
        public Submission getSubmission(
                @PathVariable("assignmentId") Long assignmentId,
                @PathVariable("userId") Long userId) {
            return submissionRepository.findByAssignmentIdAndUserId(assignmentId, userId)
                    .orElseThrow(() -> new NoSuchElementException(
                            "Submission not found for assignmentId: " + assignmentId + " and userId: " + userId));
        }
    }
}
