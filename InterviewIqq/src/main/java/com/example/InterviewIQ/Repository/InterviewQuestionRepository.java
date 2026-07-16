package com.example.InterviewIQ.Repository;


import com.example.InterviewIQ.Entity.InterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {
    List<InterviewQuestion> findAllBySessionId(Long sessionId);
    Optional<InterviewQuestion> findByIdAndSessionId(Long id, Long sessionId);
}
