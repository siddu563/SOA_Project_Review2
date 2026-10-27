package com.learnknow.gateway;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class JwtGatewayFilterTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-32-characters-long!";
    private SecretKey key;
    private JwtGatewayFilter filter;

    @BeforeEach
    void setUp() {
        this.key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        this.filter = new JwtGatewayFilter(SECRET);
    }

    private String createToken(Long userId, String email, String role, long validityMs) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claims(Map.of("userId", userId, "email", email, "role", role))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(validityMs)))
                .signWith(key)
                .compact();
    }

    @Test
    void testPublicEndpointsPassThroughWithoutToken() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/auth/login").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = ex -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();
        assertTrue(chainCalled.get(), "Chain filter should have been invoked for public login");
    }

    @Test
    void testMissingAuthorizationHeaderReturns401() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/courses").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = ex -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();
        assertFalse(chainCalled.get());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void testInvalidTokenReturns401() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/courses")
                .header("Authorization", "Bearer invalid-tampered-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = ex -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();
        assertFalse(chainCalled.get());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void testExpiredTokenReturns401() {
        String expiredToken = createToken(1L, "student@test.com", "STUDENT", -10000);
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/courses")
                .header("Authorization", "Bearer " + expiredToken)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = ex -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();
        assertFalse(chainCalled.get());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void testValidTokenPropagatesHeadersToDownstream() {
        String token = createToken(42L, "alex@example.com", "STUDENT", 60000);
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/courses")
                .header("Authorization", "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = ex -> {
            chainCalled.set(true);
            assertEquals("42", ex.getRequest().getHeaders().getFirst("X-User-Id"));
            assertEquals("alex@example.com", ex.getRequest().getHeaders().getFirst("X-User-Email"));
            assertEquals("STUDENT", ex.getRequest().getHeaders().getFirst("X-User-Role"));
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();
        assertTrue(chainCalled.get(), "Downstream chain should have been called with propagated headers");
    }

    @Test
    void testStudentCreatingCourseIsForbidden403() {
        String studentToken = createToken(10L, "student@example.com", "STUDENT", 60000);
        MockServerHttpRequest request = MockServerHttpRequest.method(HttpMethod.POST, "/api/courses")
                .header("Authorization", "Bearer " + studentToken)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = ex -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();
        assertFalse(chainCalled.get(), "Student must not be allowed to POST courses");
        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    }

    @Test
    void testAdminCreatingCourseIsAllowed() {
        String adminToken = createToken(1L, "admin@example.com", "ADMIN", 60000);
        MockServerHttpRequest request = MockServerHttpRequest.method(HttpMethod.POST, "/api/courses")
                .header("Authorization", "Bearer " + adminToken)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = ex -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();
        assertTrue(chainCalled.get(), "Admin must be allowed to POST courses");
    }

    @Test
    void testInstructorCreatingAssignmentIsAllowed() {
        String instructorToken = createToken(2L, "teacher@example.com", "INSTRUCTOR", 60000);
        MockServerHttpRequest request = MockServerHttpRequest.method(HttpMethod.POST, "/api/assignments")
                .header("Authorization", "Bearer " + instructorToken)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = ex -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();
        assertTrue(chainCalled.get(), "Instructor must be allowed to POST assignments");
    }

    @Test
    void testStudentSubmittingAssignmentIsAllowed() {
        String studentToken = createToken(5L, "student@example.com", "STUDENT", 60000);
        MockServerHttpRequest request = MockServerHttpRequest.method(HttpMethod.POST, "/api/assignments/1/submit")
                .header("Authorization", "Bearer " + studentToken)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = ex -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();
        assertTrue(chainCalled.get(), "Student submitting assignment must be allowed");
    }
}
