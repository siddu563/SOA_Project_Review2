package com.learnknow.access;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<AccessServiceApplication.Enrollment, Long> {
    List<AccessServiceApplication.Enrollment> findByUserId(Long id);
    Optional<AccessServiceApplication.Enrollment> findByUserIdAndCourseId(Long userId, Long courseId);
}
