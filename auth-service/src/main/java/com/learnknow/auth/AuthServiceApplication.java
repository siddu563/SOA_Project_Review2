package com.learnknow.auth;

import com.learnknow.auth.dto.LoginRequest;
import com.learnknow.auth.dto.LoginResponse;
import com.learnknow.auth.dto.RegisterRequest;
import com.learnknow.auth.dto.UserResponse;
import com.learnknow.auth.model.User;
import com.learnknow.auth.repository.UserRepository;
import com.learnknow.auth.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @RestController
    @RequestMapping("/api/auth")
    public static class AuthController {
        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtService jwtService;

        public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
            this.userRepository = userRepository;
            this.passwordEncoder = passwordEncoder;
            this.jwtService = jwtService;
        }

        @PostMapping("/register")
        @ResponseStatus(HttpStatus.CREATED)
        public UserResponse register(@Valid @RequestBody RegisterRequest request) {
            if (userRepository.existsByEmail(request.email().trim().toLowerCase())) {
                throw new IllegalArgumentException("Email is already registered");
            }
            User user = new User();
            user.name = request.name().trim();
            user.email = request.email().trim().toLowerCase();
            user.password = passwordEncoder.encode(request.password());
            user.role = (request.role() == null || request.role().isBlank()) ? "STUDENT" : request.role().trim().toUpperCase();

            User saved = userRepository.save(user);
            return toResponse(saved);
        }

        @PostMapping("/login")
        public LoginResponse login(@Valid @RequestBody LoginRequest request) {
            String email = request.email().trim().toLowerCase();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

            if (!passwordEncoder.matches(request.password(), user.password)) {
                throw new BadCredentialsException("Invalid email or password");
            }

            String token = jwtService.generateToken(user.id, user.email, user.role);
            return new LoginResponse(token, "Bearer", toResponse(user));
        }

        @GetMapping("/users/{id}")
        public UserResponse getUser(@PathVariable("id") Long id) {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new NoSuchElementException("User not found with id: " + id));
            return toResponse(user);
        }

        private UserResponse toResponse(User user) {
            return new UserResponse(user.id, user.name, user.email, user.role, user.createdAt);
        }
    }
}
