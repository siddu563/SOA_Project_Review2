package com.learnknow.auth.dto;

import java.time.Instant;

public record UserResponse(
    Long id,
    String name,
    String email,
    String role,
    Instant createdAt
) {}
