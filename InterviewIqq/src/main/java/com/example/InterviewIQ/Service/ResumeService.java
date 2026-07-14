package com.example.InterviewIQ.Service;

import com.example.InterviewIQ.Dto.ResumeDTO;
import com.example.InterviewIQ.Entity.Resume;
import com.example.InterviewIQ.Entity.User;
import com.example.InterviewIQ.Exception.ResourceNotFoundException;
import com.example.InterviewIQ.Repository.ResumeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Transactional
    public ResumeDTO uploadAndProcess(MultipartFile file, User currentUser) throws IOException {
        // 1. Validate
        validatePdfFile(file);

        // 2. Save file to disk
        String savedPath = saveFileToDisk(file);

        // 3. Extract text from PDF
        String rawText = extractTextFromPdf(savedPath);
        log.debug("Extracted {} characters from PDF", rawText.length());

        // 4. Call Gemini to extract skills
        GeminiService.SkillExtractionResult extraction = geminiService.extractSkills(rawText);
        log.debug("Gemini extracted {} skills", extraction.getSkills().size());

        // 5. Save to DB
        Resume resume = Resume.builder()
                .user(currentUser)
                .fileName(file.getOriginalFilename())
                .filePath(savedPath)
                .extractedSkills(objectMapper.writeValueAsString(extraction.getSkills()))
                .experienceLevel(extraction.getExperienceLevel())
                .jobRoles(objectMapper.writeValueAsString(extraction.getJobRoles()))
                .rawText(rawText)
                .build();

        resumeRepository.save(resume);

        return toDTO(resume, extraction.getSkills(), extraction.getJobRoles());
    }

    public List<ResumeDTO> getUserResumes(User currentUser) {
        return resumeRepository
                .findAllByUserIdOrderByUploadedAtDesc(currentUser.getId())
                .stream()
                .map(this::toDTOFromEntity)
                .toList();
    }

    public ResumeDTO getResumeById(Long resumeId, User currentUser) {
        Resume resume = resumeRepository.findByIdAndUserId(resumeId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Resume not found or access denied"));
        return toDTOFromEntity(resume);
    }

    public void deleteResume(Long resumeId, User currentUser) {
        Resume resume = resumeRepository.findByIdAndUserId(resumeId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found"));
        // Delete file from disk
        try { Files.deleteIfExists(Paths.get(resume.getFilePath())); }
        catch (IOException e) { log.warn("Could not delete file: {}", resume.getFilePath()); }
        resumeRepository.delete(resume);
    }

    // ==========================================
    // PRIVATE HELPERS
    // ==========================================

    private void validatePdfFile(MultipartFile file) {
        if (file.isEmpty()) throw new IllegalArgumentException("File is empty");
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new IllegalArgumentException("Only PDF files are accepted");
        }
        if (file.getSize() > 10 * 1024 * 1024) { // 10MB
            throw new IllegalArgumentException("File size must not exceed 10MB");
        }
    }

    private String saveFileToDisk(MultipartFile file) throws IOException {
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        // UUID prevents filename conflicts and directory traversal attacks
        String uniqueFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(uniqueFileName);
        Files.write(filePath, file.getBytes());
        return filePath.toString();
    }

    private String extractTextFromPdf(String filePath) throws IOException {
        try (PDDocument document = Loader.loadPDF(new File(filePath))) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            return text.length() > 8000 ? text.substring(0, 8000) : text;
        }
    }

    @SuppressWarnings("unchecked")
    private ResumeDTO toDTOFromEntity(Resume resume) {
        try {
            List<String> skills = objectMapper.readValue(
                    resume.getExtractedSkills() != null ? resume.getExtractedSkills() : "[]",
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            List<String> roles = objectMapper.readValue(
                    resume.getJobRoles() != null ? resume.getJobRoles() : "[]",
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            return toDTO(resume, skills, roles);
        } catch (Exception e) {
            return toDTO(resume, List.of(), List.of());
        }
    }

    private ResumeDTO toDTO(Resume resume, List<String> skills, List<String> roles) {
        return ResumeDTO.builder()
                .id(resume.getId())
                .fileName(resume.getFileName())
                .extractedSkills(skills)
                .experienceLevel(resume.getExperienceLevel())
                .jobRoles(roles)
                .uploadedAt(resume.getUploadedAt())
                .build();
    }
}
