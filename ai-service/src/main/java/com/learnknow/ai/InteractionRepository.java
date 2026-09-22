package com.learnknow.ai;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InteractionRepository extends JpaRepository<AiServiceApplication.AIInteraction, Long> {
}
