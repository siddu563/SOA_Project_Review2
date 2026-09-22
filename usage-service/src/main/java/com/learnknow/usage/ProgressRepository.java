package com.learnknow.usage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProgressRepository extends JpaRepository<UsageServiceApplication.Progress, Long> {
    Optional<UsageServiceApplication.Progress> findByUserIdAndCourseId(Long userId, Long courseId);
}
