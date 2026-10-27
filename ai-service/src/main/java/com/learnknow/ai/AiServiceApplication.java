package com.learnknow.ai;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@SpringBootApplication
public class AiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiServiceApplication.class, args);
    }

    @Entity
    @Table(name = "ai_interactions")
    public static class AIInteraction {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        public Long id;

        @Column(nullable = false)
        public Long userId;

        @Column(columnDefinition = "TEXT", nullable = false)
        public String question;

        @Column(columnDefinition = "TEXT", nullable = false)
        public String response;

        public Instant timestamp = Instant.now();
    }

    public record AskRequest(
            @NotNull(message = "userId must not be null") Long userId,
            @NotBlank(message = "question must not be blank") String question
    ) {}

    public record AskResponse(
            Long id,
            String question,
            String response,
            Instant timestamp
    ) {}

    @RestController
    @RequestMapping("/api/ai")
    public static class AiController {
        private final InteractionRepository repository;

        public AiController(InteractionRepository repository) {
            this.repository = repository;
        }

        @PostMapping("/ask")
        public AskResponse ask(@Valid @RequestBody AskRequest r) {
            String q = r.question() == null ? "" : r.question().trim().toLowerCase();
            String answer;

            if (q.contains("nullpointer")) {
                answer = "A NullPointerException occurs when Java code tries to use an object reference that is null. Check the object reference before calling methods or fields, use Optional<T> where appropriate, and inspect the stack trace to identify the null assignment.";
            } else if (q.contains("recursion")) {
                answer = "Recursion occurs when a method calls itself. Always ensure: 1) A clear base case to terminate execution, and 2) Each recursive step progresses closer to the base case to prevent StackOverflowError.";
            } else if (q.contains("rest") || q.contains("api")) {
                answer = "A REST API exposes modular resources over HTTP. Best practices: use plural nouns in URLs (/api/courses), standard HTTP methods (GET, POST, PUT, DELETE), proper status codes (200, 201, 400, 401, 403, 404), and JSON representations.";
            } else if (q.contains("exception") || q.contains("error")) {
                answer = "Exceptions in Java represent unexpected conditions. Use checked exceptions for recoverable business conditions and unchecked (RuntimeException) for programming errors. Always log or wrap exceptions preserving the root cause.";
            } else {
                answer = "To solve this programming problem, break it down into inputs, expected outputs, edge cases, and small verifiable steps. Test each component independently with unit tests.";
            }

            AIInteraction interaction = new AIInteraction();
            interaction.userId = r.userId();
            interaction.question = r.question().trim();
            interaction.response = answer;
            interaction.timestamp = Instant.now();

            AIInteraction saved = repository.save(interaction);
            return new AskResponse(saved.id, saved.question, saved.response, saved.timestamp);
        }
    }
}
