package com.resumade.notification.security;

import java.io.IOException;
import java.util.ArrayList;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    @Value("${jwt.secret}")
    private String secretKey;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7);
            final Claims claims = extractClaims(jwt);
            final String userEmail = claims.getSubject();
            
            // Safely extract userId which might be parsed as Long or Integer
            Object userIdObj = claims.get("userId");
            final Integer userId = (userIdObj instanceof Number) ? ((Number) userIdObj).intValue() : null;

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Preserve the raw JWT as the credentials so downstream services
                // (like broadcast logic) can forward it when calling other services.
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userEmail, jwt, new ArrayList<>()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                request.setAttribute("userId", userId);
            }
        } catch (Exception e) {
            log.error("JWT Authentication failed: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
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
            log.error("Failed to parse JWT claims: {}", e.getMessage());
            throw e;
        }
    }
}
