package com.example.InterviewIQ.Entity;
import jakarta.persistence.*;
        import lombok.*;
        import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "interview_questions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many questions → one session
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private InterviewSession session;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String category = "TECHNICAL";

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String difficulty = "MEDIUM";

    // Set when user submits their answer
    @Column(name = "user_answer", columnDefinition = "TEXT")
    private String userAnswer;

    // Set after Gemini evaluates the answer
    @Column(name = "ai_feedback", columnDefinition = "TEXT")
    private String aiFeedback;

    // JSON array of strengths: ["Good structure", "Concise"]
    @Column(columnDefinition = "TEXT")
    private String strengths;

    // JSON array of improvements: ["Add more examples", "Be specific"]
    @Column(columnDefinition = "TEXT")
    private String improvements;

    // Score from 0 to 10
    @Column(precision = 4, scale = 1)
    private BigDecimal score;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}