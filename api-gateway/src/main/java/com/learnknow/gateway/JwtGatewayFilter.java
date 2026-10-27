package com.learnknow.gateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
public class JwtGatewayFilter implements GlobalFilter, Ordered {

    private final SecretKey key;

    public JwtGatewayFilter(@Value("${learnknow.jwt.secret:change-this-learnknow-secret-key-32chars}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        HttpMethod method = request.getMethod();

        // 1. Whitelist public endpoints
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // 2. Validate Authorization header
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return writeErrorResponse(
                    exchange,
                    HttpStatus.UNAUTHORIZED,
                    "Unauthorized",
                    "Missing or malformed Authorization header. Expected 'Bearer <token>'"
            );
        }

        String token = authHeader.substring(7).trim();
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException ex) {
            return writeErrorResponse(
                    exchange,
                    HttpStatus.UNAUTHORIZED,
                    "Unauthorized",
                    "JWT token has expired"
            );
        } catch (JwtException | IllegalArgumentException ex) {
            return writeErrorResponse(
                    exchange,
                    HttpStatus.UNAUTHORIZED,
                    "Unauthorized",
                    "Invalid JWT token signature or format"
            );
        }

        // 3. Extract claims
        Object userIdObj = claims.get("userId");
        String userId = userIdObj != null ? String.valueOf(userIdObj) : "";
        String email = claims.getSubject() != null ? claims.getSubject() : String.valueOf(claims.get("email"));
        String role = claims.get("role") != null ? String.valueOf(claims.get("role")).toUpperCase() : "STUDENT";

        // 4. Role-based Access Control (RBAC)
        if (isAdminOnlyOperation(path, method)) {
            if (!"ADMIN".equals(role) && !"INSTRUCTOR".equals(role)) {
                return writeErrorResponse(
                        exchange,
                        HttpStatus.FORBIDDEN,
                        "Forbidden",
                        "Access denied: Role '" + role + "' does not have permission for this operation (requires ADMIN or INSTRUCTOR)"
                );
            }
        }

        // 5. Mutate request headers with caller identity for downstream microservices
        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-User-Id", userId)
                .header("X-User-Email", email)
                .header("X-User-Role", role)
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean isPublicPath(String path) {
        return path.equals("/api/auth/register")
                || path.equals("/api/auth/login")
                || path.startsWith("/actuator")
                || path.equals("/")
                || path.equals("/favicon.ico");
    }

    private boolean isAdminOnlyOperation(String path, HttpMethod method) {
        // Course management: Creating, modifying, or deleting courses, modules, resources requires ADMIN/INSTRUCTOR
        if (path.startsWith("/api/courses")) {
            if (HttpMethod.POST.equals(method) || HttpMethod.PUT.equals(method) || HttpMethod.DELETE.equals(method)) {
                return true;
            }
        }

        // Assignment creation: POST /api/assignments (not submit)
        if (path.equals("/api/assignments") && HttpMethod.POST.equals(method)) {
            return true;
        }

        return false;
    }

    private Mono<Void> writeErrorResponse(ServerWebExchange exchange, HttpStatus status, String error, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String path = exchange.getRequest().getPath().value();
        String json = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                Instant.now().toString(),
                status.value(),
                error,
                escapeJson(message),
                escapeJson(path)
        );

        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    private String escapeJson(String raw) {
        if (raw == null) return "";
        return raw.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @Override
    public int getOrder() {
        return -100;
    }
}