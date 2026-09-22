package com.learnknow.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import com.learnknow.auth.model.User;
import com.learnknow.auth.repository.UserRepository;
import java.time.Instant;
import java.util.Map;

@SpringBootApplication
@Import(com.learnknow.auth.config.SecurityConfig.class)
public class AuthServiceApplication {
    public static void main(String[] args) { SpringApplication.run(AuthServiceApplication.class, args); }

    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    public record RegisterRequest(@NotBlank String name, @Email @NotBlank String email, @NotBlank String password, String role) {}
    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}
    public record UserResponse(Long id, String name, String email, String role, Instant createdAt) {}
    public record LoginResponse(String token, UserResponse user) {}

    @RestController @RequestMapping("/api/auth")
    public static class AuthController {
        private final UserRepository users; private final PasswordEncoder encoder; private final byte[] secret; private final long expiration;
        AuthController(UserRepository users, PasswordEncoder encoder, org.springframework.core.env.Environment env) { this.users = users; this.encoder = encoder; secret = env.getProperty("learnknow.jwt.secret", "change-this-learnknow-secret-key-32chars").getBytes(); expiration = env.getProperty("learnknow.jwt.expiration-ms", Long.class, 86400000L); }
        @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) UserResponse register(@Valid @RequestBody RegisterRequest request) { if (users.existsByEmail(request.email())) throw new IllegalArgumentException("Email is already registered"); User u = new User(); u.name=request.name(); u.email=request.email(); u.password=encoder.encode(request.password()); u.role=request.role()==null || request.role().isBlank()?"STUDENT":request.role().toUpperCase(); return response(users.save(u)); }
        @PostMapping("/login") LoginResponse login(@Valid @RequestBody LoginRequest request) { User u=users.findByEmail(request.email()).orElseThrow(() -> new IllegalArgumentException("Invalid email or password")); if(!encoder.matches(request.password(),u.password)) throw new IllegalArgumentException("Invalid email or password"); String token=Jwts.builder().claims(Map.of("userId",u.id,"email",u.email,"role",u.role)).subject(u.email).issuedAt(java.util.Date.from(Instant.now())).expiration(java.util.Date.from(Instant.now().plusMillis(expiration))).signWith(Keys.hmacShaKeyFor(secret)).compact(); return new LoginResponse(token,response(u)); }
        @GetMapping("/users/{id}") UserResponse user(@PathVariable Long id) { return response(users.findById(id).orElseThrow(() -> new java.util.NoSuchElementException("User not found"))); }
        private UserResponse response(User u) { return new UserResponse(u.id,u.name,u.email,u.role,u.createdAt); }
    }

    @RestControllerAdvice public static class Errors { @ExceptionHandler(IllegalArgumentException.class) @ResponseStatus(HttpStatus.BAD_REQUEST) Map<String,String> bad(Exception e){return Map.of("error",e.getMessage());} @ExceptionHandler(java.util.NoSuchElementException.class) @ResponseStatus(HttpStatus.NOT_FOUND) Map<String,String> missing(Exception e){return Map.of("error",e.getMessage());} }
}
