package com.learnknow.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Name must not be blank")
    String name,

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email format is invalid")
    String email,

    @NotBlank(message = "Password must not be blank")
    @Size(min = 6, message = "Password must have at least 6 characters")
    String password,

    String role
) {}
