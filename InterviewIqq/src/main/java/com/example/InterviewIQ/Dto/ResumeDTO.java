package com.example.InterviewIQ.Dto;


import lombok.*;
        import java.time.LocalDateTime;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeDTO {
    private Long id;
    private String fileName;
    private List<String> extractedSkills;
    private String experienceLevel;
    private List<String> jobRoles;
    private LocalDateTime uploadedAt;
}
