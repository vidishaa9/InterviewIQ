package com.example.InterviewIQ.Repository;


import com.example.InterviewIQ.Entity.Analytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AnalyticsRepository extends JpaRepository<Analytics, Long> {
    Optional<Analytics> findByUserId(Long userId);
}
