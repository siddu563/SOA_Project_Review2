package com.learnknow.usage;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

@SpringBootApplication
public class UsageServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UsageServiceApplication.class, args);
    }

    @Entity
    @Table(name = "user_progress")
    public static class Progress {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        public Long id;

        @Column(nullable = false)
        public Long userId;

        @Column(nullable = false)
        public Long courseId;

        public Long moduleId;

        @Column(nullable = false)
        public double completionPercent;

        public Instant updatedAt = Instant.now();
    }

    @Entity
    @Table(name = "usage_history")
    public static class UsageHistory {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        public Long id;

        @Column(nullable = false)
        public Long userId;

        public Long resourceId;

        @Column(nullable = false)
        public String action;

        public Instant timestamp = Instant.now();
    }

    public record ActivityRequest(
            @NotNull(message = "userId must not be null") Long userId,
            Long resourceId,
            @NotBlank(message = "action must not be blank") String action
    ) {}

    public record ProgressRequest(
            @NotNull(message = "userId must not be null") Long userId,
            @NotNull(message = "courseId must not be null") Long courseId,
            Long moduleId,
            @NotNull(message = "completionPercent must not be null")
            @DecimalMin(value = "0.0", message = "completionPercent must be >= 0.0")
            @DecimalMax(value = "100.0", message = "completionPercent must be <= 100.0")
            Double completionPercent
    ) {}

    @RestController
    @RequestMapping("/api/usage")
    public static class UsageController {
        private final ProgressRepository progressRepository;
        private final HistoryRepository historyRepository;

        public UsageController(ProgressRepository progressRepository, HistoryRepository historyRepository) {
            this.progressRepository = progressRepository;
            this.historyRepository = historyRepository;
        }

        @PostMapping("/activity")
        @ResponseStatus(HttpStatus.CREATED)
        public UsageHistory recordActivity(@Valid @RequestBody ActivityRequest r) {
            UsageHistory x = new UsageHistory();
            x.userId = r.userId();
            x.resourceId = r.resourceId();
            x.action = r.action().trim();
            x.timestamp = Instant.now();
            return historyRepository.save(x);
        }

        @GetMapping("/history/{userId}")
        public List<UsageHistory> getHistory(@PathVariable("userId") Long userId) {
            return historyRepository.findByUserIdOrderByTimestampDesc(userId);
        }

        @PostMapping("/progress")
        public Progress recordProgress(@Valid @RequestBody ProgressRequest r) {
            if (r.completionPercent() == null || r.completionPercent() < 0.0 || r.completionPercent() > 100.0) {
                throw new IllegalArgumentException("completionPercent must be between 0 and 100");
            }
            Progress p = progressRepository.findByUserIdAndCourseId(r.userId(), r.courseId())
                    .orElseGet(Progress::new);
            p.userId = r.userId();
            p.courseId = r.courseId();
            p.moduleId = r.moduleId();
            p.completionPercent = r.completionPercent();
            p.updatedAt = Instant.now();
            return progressRepository.save(p);
        }

        @GetMapping("/progress/{userId}/{courseId}")
        public Progress getProgress(
                @PathVariable("userId") Long userId,
                @PathVariable("courseId") Long courseId) {
            return progressRepository.findByUserIdAndCourseId(userId, courseId)
                    .orElseThrow(() -> new NoSuchElementException(
                            "Progress not found for userId: " + userId + " and courseId: " + courseId));
        }
    }
}
