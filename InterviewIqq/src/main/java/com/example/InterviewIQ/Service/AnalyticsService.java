package com.example.InterviewIQ.Service;

import com.example.InterviewIQ.Entity.Analytics;
import com.example.InterviewIQ.Entity.User;
import com.example.InterviewIQ.Repository.AnalyticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AnalyticsRepository analyticsRepository;

    @Transactional
    public void incrementTotalSessions(User user) {
        Analytics analytics = getOrCreate(user);
        analytics.setTotalSessions(analytics.getTotalSessions() + 1);
        analytics.setUpdatedAt(LocalDateTime.now());
        analyticsRepository.save(analytics);
    }

    @Transactional
    public void updateAfterCompletion(User user, BigDecimal sessionScore) {
        Analytics analytics = getOrCreate(user);

        int newCompletedCount = analytics.getCompletedSessions() + 1;

        // Rolling average: prevents needing all historical scores
        BigDecimal currentTotal = analytics.getAverageScore()
                .multiply(BigDecimal.valueOf(analytics.getCompletedSessions()));
        BigDecimal newAverage = currentTotal.add(sessionScore)
                .divide(BigDecimal.valueOf(newCompletedCount), 2, RoundingMode.HALF_UP);

        analytics.setCompletedSessions(newCompletedCount);
        analytics.setAverageScore(newAverage);

        if (sessionScore.compareTo(analytics.getBestScore()) > 0) {
            analytics.setBestScore(sessionScore);
        }

        analytics.setUpdatedAt(LocalDateTime.now());
        analyticsRepository.save(analytics);
    }

    public Analytics getAnalytics(User user) {
        return getOrCreate(user);
    }

    private Analytics getOrCreate(User user) {
        return analyticsRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Analytics a = Analytics.builder().user(user).build();
                    return analyticsRepository.save(a);
                });
    }
}
