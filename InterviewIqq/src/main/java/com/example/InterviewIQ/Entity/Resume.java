package com.example.InterviewIQ.Entity;

import jakarta.persistence.*;
        import lombok.*;
        import java.time.LocalDateTime;

@Entity
@Table(name = "resumes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many resumes → one user. LAZY = don't load user data unless accessed
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    // JSON array stored as text: ["Java","Spring Boot","React"]
    @Column(name = "extracted_skills", columnDefinition = "TEXT")
    private String extractedSkills;

    @Column(name = "experience_level", length = 20)
    private String experienceLevel;

    // JSON array: ["Backend Developer", "Full Stack Developer"]
    @Column(name = "job_roles", columnDefinition = "TEXT")
    private String jobRoles;

    // Full PDF text — used as input for Gemini
    @Column(name = "raw_text", columnDefinition = "TEXT")
    private String rawText;

    @Column(name = "uploaded_at", nullable = false)
    @Builder.Default
    private LocalDateTime uploadedAt = LocalDateTime.now();
}
