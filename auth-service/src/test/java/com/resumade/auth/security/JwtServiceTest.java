package com.resumade.auth.security;

import com.resumade.auth.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails userDetails;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Use a long enough key for HS256 (at least 256 bits / 32 characters)
        ReflectionTestUtils.setField(jwtService, "secretKey", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600000L);

        userDetails = new org.springframework.security.core.userdetails.User(
                "john@example.com",
                "password",
                java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))
        );

        user = new User();
        user.setUserId(1);
        user.setRole(User.Role.USER);
        user.setSubscriptionPlan(User.SubscriptionPlan.FREE);
        user.setFullName("John Doe");
    }

    @Test
    void generateToken_ExtractUsername_Matches() {
        String token = jwtService.generateToken(userDetails, user);
        assertNotNull(token);
        assertEquals("john@example.com", jwtService.extractUsername(token));
    }

    @Test
    void validateToken_ReturnsTrue_ForValidToken() {
        String token = jwtService.generateToken(userDetails, user);
        assertTrue(jwtService.validateToken(token, userDetails));
    }

    @Test
    void validateToken_ReturnsFalse_ForDifferentUser() {
        String token = jwtService.generateToken(userDetails, user);
        UserDetails otherUser = mock(UserDetails.class);
        when(otherUser.getUsername()).thenReturn("other@example.com");
        assertFalse(jwtService.validateToken(token, otherUser));
    }

    @Test
    void extractClaim_WorksForCustomClaims() {
        String token = jwtService.generateToken(userDetails, user);
        Integer userId = jwtService.extractClaim(token, claims -> claims.get("userId", Integer.class));
        assertEquals(1, userId);
    }
}
