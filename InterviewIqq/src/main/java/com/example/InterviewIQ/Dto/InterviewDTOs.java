package com.example.InterviewIQ.Dto;

import lombok.*;
        import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


public class InterviewDTOs {

    // POST /api/interviews/start
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class StartSessionRequest {
        private Long resumeId;       // Which resume to base questions on
        private String targetRole;   // Optional: "Backend Developer"
    }

    // POST /api/interviews/{sessionId}/questions/{questionId}/answer
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SubmitAnswerRequest {
        private String answer;       // User's answer text
    }

    // Response for a single question
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class QuestionDTO {
        private Long id;
        private String questionText;
        private String category;
        private String difficulty;
        private String userAnswer;
        private String aiFeedback;
        private List<String> strengths;
        private List<String> improvements;
        private BigDecimal score;
        private boolean answered;
    }

    // Response for a full session
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SessionDTO {
        private Long id;
        private Long resumeId;
        private String status;
        private BigDecimal overallScore;
        private Integer totalQuestions;
        private Integer answeredQuestions;
        private List<QuestionDTO> questions;
        private LocalDateTime createdAt;
        private LocalDateTime completedAt;
    }

    // Summary for history list (lighter than full SessionDTO)
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SessionSummaryDTO {
        private Long id;
        private String status;
        private BigDecimal overallScore;
        private Integer totalQuestions;
        private LocalDateTime createdAt;
        private String resumeFileName;
    }
}