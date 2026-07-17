package com.example.InterviewIQ.Security;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Read the Authorization header
        final String authHeader = request.getHeader("Authorization");

        // If no Bearer token, just continue without authenticating
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extract the token (skip "Bearer " prefix which is 7 characters)
        final String jwt = authHeader.substring(7);

        // 3. Extract the email from the token
        final String userEmail = jwtService.extractUsername(jwt);

        // 4. Only authenticate if we have a user and they aren't already authenticated
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Load user from database
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            // Validate token
            if (jwtService.isTokenValid(jwt, userDetails)) {
                // Create authentication token (Spring Security's internal object)
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Set in SecurityContext — "this request is authenticated as this user"
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 5. Continue to the next filter (eventually reaches your controller)
        filterChain.doFilter(request, response);
    }
}
