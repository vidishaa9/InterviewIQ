package com.example.InterviewIQ.Exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
        import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

        import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 404 - Resource not found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    // 409 - Email already exists on register
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailExists(EmailAlreadyExistsException ex) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    // 401 - Wrong email or password on login
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return buildError(HttpStatus.UNAUTHORIZED, "Invalid email or password", null);
    }

    // 400 - @Valid annotation triggered validation failure
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });
        return buildError(HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors);
    }

    // 400 - Business rule violations (e.g., wrong session state)
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    // 400 - Bad input (e.g., non-PDF file uploaded)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    // 500 - Unexpected errors (Gemini down, DB down, etc.)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Unexpected error: ", ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again.", null);
    }

    private ResponseEntity<ErrorResponse> buildError(
            HttpStatus status, String message, Map<String, String> errors) {
        ErrorResponse error = new ErrorResponse(
                status.value(), message, errors, LocalDateTime.now());
        return ResponseEntity.status(status).body(error);
    }

    // The consistent error response shape
    public record ErrorResponse(
            int status,
            String message,
            Map<String, String> errors,
            LocalDateTime timestamp
    ) {}
}
