package com.example.InterviewIQ.Service;


import com.example.InterviewIQ.Dto.InterviewDTOs;
import com.example.InterviewIQ.Entity.InterviewQuestion;
import com.example.InterviewIQ.Entity.InterviewSession;
import com.example.InterviewIQ.Entity.Resume;
import com.example.InterviewIQ.Entity.User;
import com.example.InterviewIQ.Enum.SessionStatus;
import com.example.InterviewIQ.Repository.InterviewQuestionRepository;
import com.example.InterviewIQ.Repository.InterviewSessionRepository;
import com.example.InterviewIQ.Repository.ResumeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewService {

    private final InterviewSessionRepository sessionRepository;
    private final InterviewQuestionRepository questionRepository;
    private final ResumeRepository resumeRepository;
    private final GeminiService geminiService;
    private final AnalyticsService analyticsService;
    private final ObjectMapper objectMapper;

    // ==========================================
    // 1. START SESSION — Generate questions
    // ==========================================
    @Transactional
    public InterviewDTOs.SessionDTO startSession(
            InterviewDTOs.StartSessionRequest request, User currentUser) throws Exception {

        // Load the resume (validates ownership)
        Resume resume = resumeRepository.findByIdAndUserId(request.getResumeId(), currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found"));

        // Parse stored skills JSON
        List<String> skills = objectMapper.readValue(
                resume.getExtractedSkills() != null ? resume.getExtractedSkills() : "[]",
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));

        String targetRole = request.getTargetRole() != null
                ? request.getTargetRole()
                : (resume.getJobRoles() != null ? resume.getJobRoles() : "Software Developer");

        // Generate questions from Gemini
        List<GeminiService.GeneratedQuestion> generatedQuestions =
                geminiService.generateQuestions(skills, resume.getExperienceLevel(), targetRole);

        // Create session
        InterviewSession session = InterviewSession.builder()
                .user(currentUser)
                .resume(resume)
                .status(SessionStatus.PENDING)
                .totalQuestions(generatedQuestions.size())
                .build();

        // Create question entities
        List<InterviewQuestion> questions = generatedQuestions.stream()
                .map(gq -> InterviewQuestion.builder()
                        .session(session)
                        .questionText(gq.getText())
                        .category(gq.getCategory())
                        .difficulty(gq.getDifficulty())
                        .build())
                .toList();

        session.setQuestions(questions);
        sessionRepository.save(session);

        // Update analytics total count
        analyticsService.incrementTotalSessions(currentUser);

        return toSessionDTO(session);
    }

    // ==========================================
    // 2. START INTERVIEW — Move to IN_PROGRESS
    // ==========================================
    @Transactional
    public InterviewDTOs.SessionDTO startInterview(Long sessionId, User currentUser) {
        InterviewSession session = getSessionForUser(sessionId, currentUser);

        if (session.getStatus() != SessionStatus.PENDING) {
            throw new IllegalStateException("Session is already " + session.getStatus());
        }

        session.setStatus(SessionStatus.IN_PROGRESS);
        sessionRepository.save(session);
        return toSessionDTO(session);
    }

    // ==========================================
    // 3. SUBMIT ANSWER — AI evaluation
    // ==========================================
    @Transactional
    public InterviewDTOs.QuestionDTO submitAnswer(
            Long sessionId, Long questionId,
            InterviewDTOs.SubmitAnswerRequest request,
            User currentUser) throws Exception {

        InterviewSession session = getSessionForUser(sessionId, currentUser);

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Session must be IN_PROGRESS to submit answers");
        }

        InterviewQuestion question = questionRepository
                .findByIdAndSessionId(questionId, sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));

        if (question.getUserAnswer() != null) {
            throw new IllegalStateException("This question has already been answered");
        }

        // Call Gemini to evaluate the answer
        GeminiService.AnswerEvaluation evaluation = geminiService.evaluateAnswer(
                question.getQuestionText(),
                request.getAnswer(),
                question.getCategory()
        );

        // Update question with answer + feedback
        question.setUserAnswer(request.getAnswer());
        question.setAiFeedback(evaluation.getFeedback());
        question.setStrengths(objectMapper.writeValueAsString(evaluation.getStrengths()));
        question.setImprovements(objectMapper.writeValueAsString(evaluation.getImprovements()));
        question.setScore(BigDecimal.valueOf(evaluation.getScore()).setScale(1, RoundingMode.HALF_UP));
        question.setAnsweredAt(LocalDateTime.now());
        questionRepository.save(question);

        return toQuestionDTO(question);
    }

    // ==========================================
    // 4. COMPLETE SESSION — Calculate final score
    // ==========================================
    @Transactional
    public InterviewDTOs.SessionDTO completeSession(Long sessionId, User currentUser) {
        InterviewSession session = getSessionForUser(sessionId, currentUser);

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Session must be IN_PROGRESS to complete");
        }

        List<InterviewQuestion> questions = questionRepository.findAllBySessionId(sessionId);

        // Calculate average score of all answered questions
        BigDecimal avgScore = questions.stream()
                .filter(q -> q.getScore() != null)
                .map(InterviewQuestion::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(Math.max(questions.size(), 1)), 2, RoundingMode.HALF_UP);

        session.setStatus(SessionStatus.COMPLETED);
        session.setOverallScore(avgScore);
        session.setCompletedAt(LocalDateTime.now());
        sessionRepository.save(session);

        // Update analytics with the new score
        analyticsService.updateAfterCompletion(currentUser, avgScore);

        return toSessionDTO(session);
    }

    // ==========================================
    // READ OPERATIONS
    // ==========================================

    public InterviewDTOs.SessionDTO getSession(Long sessionId, User currentUser) {
        return toSessionDTO(getSessionForUser(sessionId, currentUser));
    }

    public List<InterviewDTOs.SessionSummaryDTO> getHistory(User currentUser) {
        return sessionRepository.findAllByUserIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(this::toSummaryDTO)
                .toList();
    }

    // ==========================================
    // HELPERS
    // ==========================================

    private InterviewSession getSessionForUser(Long sessionId, User user) {
        return sessionRepository.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
    }

    private InterviewDTOs.SessionDTO toSessionDTO(InterviewSession session) {
        List<InterviewDTOs.QuestionDTO> questionDTOs = session.getQuestions().stream()
                .map(this::toQuestionDTO).toList();
        long answered = questionDTOs.stream().filter(InterviewDTOs.QuestionDTO::isAnswered).count();

        return InterviewDTOs.SessionDTO.builder()
                .id(session.getId())
                .resumeId(session.getResume().getId())
                .status(session.getStatus().name())
                .overallScore(session.getOverallScore())
                .totalQuestions(session.getTotalQuestions())
                .answeredQuestions((int) answered)
                .questions(questionDTOs)
                .createdAt(session.getCreatedAt())
                .completedAt(session.getCompletedAt())
                .build();
    }

    @SuppressWarnings("unchecked")
    private InterviewDTOs.QuestionDTO toQuestionDTO(InterviewQuestion q) {
        List<String> strengths = List.of();
        List<String> improvements = List.of();
        try {
            if (q.getStrengths() != null)
                strengths = objectMapper.readValue(q.getStrengths(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            if (q.getImprovements() != null)
                improvements = objectMapper.readValue(q.getImprovements(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception ignored) {}

        return InterviewDTOs.QuestionDTO.builder()
                .id(q.getId())
                .questionText(q.getQuestionText())
                .category(q.getCategory())
                .difficulty(q.getDifficulty())
                .userAnswer(q.getUserAnswer())
                .aiFeedback(q.getAiFeedback())
                .strengths(strengths)
                .improvements(improvements)
                .score(q.getScore())
                .answered(q.getUserAnswer() != null)
                .build();
    }

    private InterviewDTOs.SessionSummaryDTO toSummaryDTO(InterviewSession session) {
        return InterviewDTOs.SessionSummaryDTO.builder()
                .id(session.getId())
                .status(session.getStatus().name())
                .overallScore(session.getOverallScore())
                .totalQuestions(session.getTotalQuestions())
                .createdAt(session.getCreatedAt())
                .resumeFileName(session.getResume().getFileName())
                .build();
    }
}