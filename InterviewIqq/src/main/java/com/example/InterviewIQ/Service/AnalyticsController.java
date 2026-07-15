package com.example.InterviewIQ.Service;

import com.example.InterviewIQ.Entity.Analytics;
import com.example.InterviewIQ.Entity.User;
import lombok.*;
        import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

        import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * GET /api/analytics/me
     * Returns the current user's aggregated stats for the dashboard.
     */
    @GetMapping("/me")
    public ResponseEntity<AnalyticsDTO> getMyAnalytics(
            @AuthenticationPrincipal User currentUser) {
        Analytics analytics = analyticsService.getAnalytics(currentUser);
        return ResponseEntity.ok(toDTO(analytics));
    }

    private AnalyticsDTO toDTO(Analytics a) {
        return AnalyticsDTO.builder()
                .totalSessions(a.getTotalSessions())
                .completedSessions(a.getCompletedSessions())
                .averageScore(a.getAverageScore())
                .bestScore(a.getBestScore())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
class AnalyticsDTO {
    private Integer totalSessions;
    private Integer completedSessions;
    private BigDecimal averageScore;
    private BigDecimal bestScore;
    private LocalDateTime updatedAt;
}
