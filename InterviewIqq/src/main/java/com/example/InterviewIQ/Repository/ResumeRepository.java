package com.example.InterviewIQ.Repository;

import com.example.InterviewIQ.Entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;


@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {
    List<Resume> findAllByUserIdOrderByUploadedAtDesc(Long userId);
    Optional<Resume> findByIdAndUserId(Long id, Long userId);
}
