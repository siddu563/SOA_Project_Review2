package com.learnknow.assignment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentRepository extends JpaRepository<AssignmentServiceApplication.Assignment, Long> {
    List<AssignmentServiceApplication.Assignment> findByCourseId(Long id);
}
