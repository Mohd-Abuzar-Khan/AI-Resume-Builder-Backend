package com.resumade.gateway.filter;

import java.util.List;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;

@Component
// Global gateway filter — validates JWT, extracts claims, and injects X-User-Id/Role/Plan headers for downstream services
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Value("${jwt.secret}")
    private String secretKey;

    // WARNING: Overly permissive — /api/v1/ai, /api/v1/resumes/, /broadcast are public (see audit)
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/google",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/templates",
            "/api/v1/resumes/",
            "/api/v1/resumes/public",
            "/api/v1/notifications/broadcast",
            "/actuator",
            "/eureka",
            "/swagger-ui",
            "/swagger-ui.html",
            "/v3/api-docs",
            "/swagger-resources",
            "/webjars",
            "/auth-service/v3/api-docs",
            "/auth-service/swagger-ui",
            "/resume-service/v3/api-docs",
            "/resume-service/swagger-ui",
            "/ai-service/v3/api-docs",
            "/ai-service/swagger-ui",
            "/export-service/v3/api-docs",
            "/export-service/swagger-ui",
            "/template-service/v3/api-docs",
            "/template-service/swagger-ui",
            "/notification-service/v3/api-docs",
            "/notification-service/swagger-ui",
            "/job-match-service/v3/api-docs",
            "/job-match-service/swagger-ui",
            "/api/v1/ai",
            "/api/v1/ai/stream",
            "/api/v1/exports/download");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        log.info("Gateway filtering path: {}", path);

        if ("OPTIONS".equalsIgnoreCase(request.getMethod().name())) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        boolean hasToken = authHeader != null && authHeader.startsWith("Bearer ");

        if (isPublicPath(path)) {
            log.info("Path is public: {}", path);
            if (hasToken) {
                try {
                    String token = authHeader.substring(7);
                    Claims claims = extractClaims(token);
                    // Enriches request with user identity for downstream services even on public paths
                    ServerHttpRequest modifiedRequest = request.mutate()
                            .header("X-User-Id", String.valueOf(claims.get("userId")))
                            .header("X-User-Role", String.valueOf(claims.get("role")))
                            .header("X-User-Plan", String.valueOf(claims.get("plan")))
                            .build();
                    return chain.filter(exchange.mutate().request(modifiedRequest).build());
                } catch (Exception e) {
                    log.warn("Optional JWT parsing failed for public path {}: {}", path, e.getMessage());
                }
            }
            return chain.filter(exchange);
        }

        if (!hasToken) {
            log.warn("Missing or invalid Authorization header for protected path: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            String token = authHeader.substring(7);
            Claims claims = extractClaims(token);

            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Id", String.valueOf(claims.get("userId")))
                    .header("X-User-Role", String.valueOf(claims.get("role")))
                    .header("X-User-Plan", String.valueOf(claims.get("plan")))
                    .header("Authorization", authHeader)
                    .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        } catch (Exception e) {
            log.error("JWT validation failed for path {}: {}", path, e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }

    // Uses startsWith matching — overly broad for paths like /api/v1/resumes/ which matches all sub-paths
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private Claims extractClaims(String token) {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(
                    java.util.Base64.getEncoder().encodeToString(secretKey.getBytes()));
            SecretKey key = Keys.hmacShaKeyFor(keyBytes);

            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("Failed to parse JWT claims in gateway: {}", e.getMessage());
            throw e;
        }
    }
}
