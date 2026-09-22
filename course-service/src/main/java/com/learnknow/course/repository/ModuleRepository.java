package com.learnknow.course.repository;

import com.learnknow.course.model.Module;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModuleRepository extends JpaRepository<Module, Long> {
    List<Module> findByCourseIdOrderBySequenceNo(Long id);
}
