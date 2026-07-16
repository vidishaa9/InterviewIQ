package com.example.InterviewIQ.Controller;
import com.example.InterviewIQ.Dto.AuthDTOs;
import com.example.InterviewIQ.Service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;


    @PostMapping("/register")
    public ResponseEntity<AuthDTOs.AuthResponse> register(
            @Valid @RequestBody AuthDTOs.RegisterRequest request) {
        AuthDTOs.AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @PostMapping("/login")
    public ResponseEntity<AuthDTOs.AuthResponse> login(
            @Valid @RequestBody AuthDTOs.LoginRequest request) {
        AuthDTOs.AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}