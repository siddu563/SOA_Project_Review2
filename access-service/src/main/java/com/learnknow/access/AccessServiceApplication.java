package com.learnknow.access;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class AccessServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccessServiceApplication.class, args);
    }

    @Entity
    @Table(name = "enrollments")
    public static class Enrollment {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        public Long id;

        @Column(nullable = false)
        public Long userId;

        @Column(nullable = false)
        public Long courseId;

        @Column(nullable = false)
        public String status = "ACTIVE";

        public Instant enrolledAt = Instant.now();
    }

    public record EnrollRequest(
            @NotNull(message = "userId must not be null") Long userId,
            @NotNull(message = "courseId must not be null") Long courseId
    ) {}

    public record AccessCheckResponse(
            Long userId,
            Long courseId,
            boolean enrolled,
            String status
    ) {}

    @RestController
    @RequestMapping("/api/access")
    public static class AccessController {
        private final EnrollmentRepository repository;

        public AccessController(EnrollmentRepository repository) {
            this.repository = repository;
        }

        @PostMapping("/enroll")
        @ResponseStatus(HttpStatus.CREATED)
        public Enrollment enroll(@Valid @RequestBody EnrollRequest request) {
            Enrollment enrollment = repository.findByUserIdAndCourseId(request.userId(), request.courseId())
                    .orElseGet(Enrollment::new);
            enrollment.userId = request.userId();
            enrollment.courseId = request.courseId();
            enrollment.status = "ACTIVE";
            enrollment.enrolledAt = Instant.now();
            return repository.save(enrollment);
        }

        @GetMapping("/user/{userId}")
        public List<Enrollment> getByUser(@PathVariable("userId") Long userId) {
            return repository.findByUserId(userId);
        }

        @GetMapping("/check/{userId}/{courseId}")
        public AccessCheckResponse check(
                @PathVariable("userId") Long userId,
                @PathVariable("courseId") Long courseId) {
            return repository.findByUserIdAndCourseId(userId, courseId)
                    .map(e -> new AccessCheckResponse(userId, courseId, "ACTIVE".equalsIgnoreCase(e.status), e.status))
                    .orElseGet(() -> new AccessCheckResponse(userId, courseId, false, "NOT_ENROLLED"));
        }

        @PostMapping("/validate")
        public AccessCheckResponse validate(@Valid @RequestBody EnrollRequest request) {
            return check(request.userId(), request.courseId());
        }
    }
}
