package com.example.InterviewIQ.Enum;

/**
 * ROLE ENUM
 * Simple role system. Every registered user gets USER.
 * ADMIN can be assigned manually in DB if needed.
 *
 * In SecurityConfig, we can use these to restrict endpoints:
 * .requestMatchers("/api/admin/**").hasRole("ADMIN")
 */
public enum Role {
    USER,
    ADMIN
}
