package com.example.InterviewIQ.Entity;
import jakarta.persistence.*;
        import lombok.*;
        import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "analytics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Analytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // One analytics row per user (unique constraint in Flyway migration)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "total_sessions", nullable = false)
    @Builder.Default
    private Integer totalSessions = 0;

    @Column(name = "completed_sessions", nullable = false)
    @Builder.Default
    private Integer completedSessions = 0;

    @Column(name = "average_score", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal averageScore = BigDecimal.ZERO;

    @Column(name = "best_score", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal bestScore = BigDecimal.ZERO;

    // JSON: ["Java", "Spring Boot", "React"]
    @Column(name = "top_skills", columnDefinition = "TEXT")
    private String topSkills;

    // JSON: ["System Design", "Behavioral Questions"]
    @Column(name = "weak_areas", columnDefinition = "TEXT")
    private String weakAreas;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
