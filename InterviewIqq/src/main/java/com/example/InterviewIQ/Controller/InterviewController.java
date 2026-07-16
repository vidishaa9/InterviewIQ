package com.example.InterviewIQ.Controller;

import com.example.InterviewIQ.Dto.InterviewDTOs;
import com.example.InterviewIQ.Entity.User;
import com.example.InterviewIQ.Service.InterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
        import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

        import java.util.List;


@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping("/start")
    public ResponseEntity<InterviewDTOs.SessionDTO> startSession(
            @RequestBody InterviewDTOs.StartSessionRequest request,
            @AuthenticationPrincipal User currentUser) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(interviewService.startSession(request, currentUser));
    }

    @PostMapping("/{sessionId}/begin")
    public ResponseEntity<InterviewDTOs.SessionDTO> beginInterview(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(interviewService.startInterview(sessionId, currentUser));
    }

    @PostMapping("/{sessionId}/questions/{questionId}/answer")
    public ResponseEntity<InterviewDTOs.QuestionDTO> submitAnswer(
            @PathVariable Long sessionId,
            @PathVariable Long questionId,
            @RequestBody InterviewDTOs.SubmitAnswerRequest request,
            @AuthenticationPrincipal User currentUser) throws Exception {
        return ResponseEntity.ok(
                interviewService.submitAnswer(sessionId, questionId, request, currentUser));
    }

    @PostMapping("/{sessionId}/complete")
    public ResponseEntity<InterviewDTOs.SessionDTO> completeSession(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(interviewService.completeSession(sessionId, currentUser));
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<InterviewDTOs.SessionDTO> getSession(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(interviewService.getSession(sessionId, currentUser));
    }

    @GetMapping("/history")
    public ResponseEntity<List<InterviewDTOs.SessionSummaryDTO>> getHistory(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(interviewService.getHistory(currentUser));
    }
}
