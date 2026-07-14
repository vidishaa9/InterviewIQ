package com.example.InterviewIQ.Controller;


import com.example.InterviewIQ.Dto.ResumeDTO;
import com.example.InterviewIQ.Entity.User;
import com.example.InterviewIQ.Service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
        import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
        import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeDTO> uploadResume(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User currentUser) throws IOException {
        ResumeDTO result = resumeService.uploadAndProcess(file, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping
    public ResponseEntity<List<ResumeDTO>> getMyResumes(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(resumeService.getUserResumes(currentUser));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResumeDTO> getResume(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(resumeService.getResumeById(id, currentUser));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResume(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        resumeService.deleteResume(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
