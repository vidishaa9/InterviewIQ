package com.example.InterviewIQ.Repository;


import com.example.InterviewIQ.Entity.InterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {
    List<InterviewSession> findAllByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<InterviewSession> findByIdAndUserId(Long id, Long userId);

    // Custom JPQL query — counts completed sessions for a user
    @Query("SELECT COUNT(s) FROM InterviewSession s WHERE s.user.id = :userId AND s.status = 'COMPLETED'")
    Long countCompletedByUserId(Long userId);
}
