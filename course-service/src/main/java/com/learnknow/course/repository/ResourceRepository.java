package com.learnknow.course.repository;

import com.learnknow.course.model.Resource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
    List<Resource> findByModuleId(Long id);
}
