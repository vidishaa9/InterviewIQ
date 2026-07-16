package com.example.InterviewIQ.Service;

import com.example.InterviewIQ.Dto.AuthDTOs;
import com.example.InterviewIQ.Entity.Analytics;

import com.example.InterviewIQ.Entity.User;
import com.example.InterviewIQ.Enum.Role;
import com.example.InterviewIQ.Exception.EmailAlreadyExistsException;
import com.example.InterviewIQ.Repository.AnalyticsRepository;
import com.example.InterviewIQ.Repository.UserRepository;
import com.example.InterviewIQ.Security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AnalyticsRepository analyticsRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthDTOs.AuthResponse register(AuthDTOs.RegisterRequest request) {
        // Check duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(
                    "An account with email " + request.getEmail() + " already exists"
            );
        }

        // Build and save user
        var user = User.builder()
                .name(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();
        userRepository.save(user);

        // Create empty analytics row for this new user
        var analytics = Analytics.builder()
                .user(user)
                .totalSessions(0)
                .completedSessions(0)
                .build();
        analyticsRepository.save(analytics);

        // Generate and return token
        var token = jwtService.generateToken(user);
        return buildAuthResponse(user, token);
    }

    public AuthDTOs.AuthResponse login(AuthDTOs.LoginRequest request) {
        // This throws BadCredentialsException if email/password wrong
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // If we get here, credentials are correct
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow();

        var token = jwtService.generateToken(user);
        return buildAuthResponse(user, token);
    }

    private AuthDTOs.AuthResponse buildAuthResponse(User user, String token) {
        return AuthDTOs.AuthResponse.builder()
                .token(token)
                .name(user.getName())
                .email(user.getEmail())
                .userId(user.getId())
                .build();
    }
}
