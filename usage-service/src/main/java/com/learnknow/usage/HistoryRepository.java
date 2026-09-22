package com.learnknow.usage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoryRepository extends JpaRepository<UsageServiceApplication.UsageHistory, Long> {
    List<UsageServiceApplication.UsageHistory> findByUserIdOrderByTimestampDesc(Long userId);
}
