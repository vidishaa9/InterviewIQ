package com.example.InterviewIQ.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
        import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    @Value("${app.gemini.api-key}")
    private String apiKey;

    @Value("${app.gemini.api-url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==========================================
    // CALL 1: Extract Skills from Resume Text
    // ==========================================

    /**
     * Sends resume text to Gemini and gets back structured skill data.
     * Returns a SkillExtractionResult with skills, experience level, etc.
     */
    public SkillExtractionResult extractSkills(String resumeText) {
        String prompt = """
            You are an expert resume analyzer.
            Analyze this resume and extract all relevant information.
            
            Return ONLY valid JSON with exactly this structure (no markdown, no explanation):
            {
              "skills": ["skill1", "skill2"],
              "experience_level": "junior|mid|senior",
              "job_roles": ["role1", "role2"],
              "summary": "brief 2-3 sentence professional summary",
              "years_of_experience": 0
            }
            
            Resume text:
            """ + resumeText;

        String jsonResponse = callGemini(prompt);

        try {
            JsonNode root = objectMapper.readTree(cleanJsonResponse(jsonResponse));
            return SkillExtractionResult.builder()
                    .skills(parseStringArray(root.get("skills")))
                    .experienceLevel(getTextOrDefault(root, "experience_level", "mid"))
                    .jobRoles(parseStringArray(root.get("job_roles")))
                    .summary(getTextOrDefault(root, "summary", ""))
                    .yearsOfExperience(root.has("years_of_experience")
                            ? root.get("years_of_experience").asInt(0) : 0)
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse Gemini skill extraction response: {}", jsonResponse, e);
            return SkillExtractionResult.builder()
                    .skills(List.of("Unable to extract skills"))
                    .experienceLevel("mid")
                    .jobRoles(List.of("Software Developer"))
                    .summary("Resume processed")
                    .yearsOfExperience(0)
                    .build();
        }
    }

    // ==========================================
    // CALL 2: Generate Interview Questions
    // ==========================================

    /**
     * Takes skills + experience level and generates targeted interview questions.
     * Returns a list of GeneratedQuestion objects.
     */
    public List<GeneratedQuestion> generateQuestions(List<String> skills,
                                                     String experienceLevel,
                                                     String targetRole) {
        String skillsStr = String.join(", ", skills);
        String prompt = String.format("""
            You are an expert technical interviewer.
            Generate 8 interview questions for a candidate with these details:
            Skills: %s
            Experience Level: %s
            Target Role: %s
            
            Mix: 4 TECHNICAL questions, 2 BEHAVIORAL questions, 2 SITUATIONAL questions.
            Vary difficulty: 2 EASY, 4 MEDIUM, 2 HARD.
            
            Return ONLY valid JSON with exactly this structure (no markdown, no explanation):
            {
              "questions": [
                {
                  "text": "question text here",
                  "category": "TECHNICAL|BEHAVIORAL|SITUATIONAL",
                  "difficulty": "EASY|MEDIUM|HARD"
                }
              ]
            }
            """, skillsStr, experienceLevel, targetRole);

        String jsonResponse = callGemini(prompt);

        try {
            JsonNode root = objectMapper.readTree(cleanJsonResponse(jsonResponse));
            JsonNode questionsNode = root.get("questions");

            return objectMapper.readerForListOf(GeneratedQuestion.class)
                    .readValue(questionsNode);
        } catch (Exception e) {
            log.error("Failed to parse Gemini question generation response: {}", jsonResponse, e);
            return getDefaultQuestions();
        }
    }

    // ==========================================
    // CALL 3: Evaluate User's Answer
    // ==========================================

    /**
     * Takes a question + user's answer and gets AI feedback with a score.
     * This is called once per question when the user submits their answer.
     */
    public AnswerEvaluation evaluateAnswer(String question, String userAnswer, String category) {
        String prompt = String.format("""
            You are an expert interview coach evaluating a candidate's answer.
            
            Question: %s
            Category: %s
            Candidate's Answer: %s
            
            Evaluate the answer on: clarity, accuracy, depth, and real-world applicability.
            
            Return ONLY valid JSON with exactly this structure (no markdown, no explanation):
            {
              "score": 7.5,
              "feedback": "Overall feedback paragraph here",
              "strengths": ["strength 1", "strength 2"],
              "improvements": ["improvement 1", "improvement 2"]
            }
            
            Score must be a number from 0 to 10.
            Be constructive and specific. Mention concrete things to improve.
            """, question, category, userAnswer);

        String jsonResponse = callGemini(prompt);

        try {
            JsonNode root = objectMapper.readTree(cleanJsonResponse(jsonResponse));
            return AnswerEvaluation.builder()
                    .score(root.get("score").asDouble(5.0))
                    .feedback(getTextOrDefault(root, "feedback", "Answer evaluated."))
                    .strengths(parseStringArray(root.get("strengths")))
                    .improvements(parseStringArray(root.get("improvements")))
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse Gemini evaluation response: {}", jsonResponse, e);
            return AnswerEvaluation.builder()
                    .score(5.0)
                    .feedback("Your answer was processed. Keep practicing!")
                    .strengths(List.of("Attempted the question"))
                    .improvements(List.of("Provide more specific examples"))
                    .build();
        }
    }

    // ==========================================
    // CORE HTTP CALL TO GEMINI API
    // ==========================================

    /**
     * Makes the actual HTTP POST request to Gemini API.
     * Gemini's REST API expects a specific JSON structure.
     * Returns the raw text response from the model.
     */
    private String callGemini(String prompt) {
        String url = apiUrl + "?key=" + apiKey;

        // Gemini API request structure
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.3,        // Lower = more consistent/predictable output
                        "maxOutputTokens", 2048,
                        "topP", 0.8
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            // Navigate Gemini's nested response structure to get the text
            // Response: candidates[0].content.parts[0].text
            List candidates = (List) response.getBody().get("candidates");
            Map firstCandidate = (Map) candidates.get(0);
            Map content = (Map) firstCandidate.get("content");
            List parts = (List) content.get("parts");
            Map firstPart = (Map) parts.get(0);

            return (String) firstPart.get("text");
        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage(), e);
            throw new RuntimeException("AI service temporarily unavailable. Please try again.", e);
        }
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    /**
     * Strips markdown code fences that LLMs sometimes add around JSON.
     * Gemini might return: ```json\n{...}\n```
     * This strips it to just: {...}
     */
    private String cleanJsonResponse(String response) {
        if (response == null) return "{}";
        return response
                .replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();
    }

    private List<String> parseStringArray(JsonNode node) {
        if (node == null || !node.isArray()) return List.of();
        return objectMapper.convertValue(node,
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
    }

    private String getTextOrDefault(JsonNode root, String field, String defaultValue) {
        return root.has(field) ? root.get(field).asText(defaultValue) : defaultValue;
    }

    private List<GeneratedQuestion> getDefaultQuestions() {
        return List.of(
                new GeneratedQuestion("Tell me about yourself and your background.", "BEHAVIORAL", "EASY"),
                new GeneratedQuestion("What is object-oriented programming?", "TECHNICAL", "EASY"),
                new GeneratedQuestion("Describe a challenging project you worked on.", "SITUATIONAL", "MEDIUM"),
                new GeneratedQuestion("Explain RESTful API design principles.", "TECHNICAL", "MEDIUM"),
                new GeneratedQuestion("How do you handle conflicts in a team?", "BEHAVIORAL", "MEDIUM"),
                new GeneratedQuestion("What is the difference between SQL and NoSQL?", "TECHNICAL", "MEDIUM"),
                new GeneratedQuestion("Describe your approach to debugging.", "SITUATIONAL", "MEDIUM"),
                new GeneratedQuestion("Explain your system design for a URL shortener.", "TECHNICAL", "HARD")
        );
    }

    // ==========================================
    // INNER RESULT CLASSES
    // ==========================================

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SkillExtractionResult {
        private List<String> skills;
        private String experienceLevel;
        private List<String> jobRoles;
        private String summary;
        private Integer yearsOfExperience;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class GeneratedQuestion {
        private String text;
        private String category;
        private String difficulty;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AnswerEvaluation {
        private Double score;
        private String feedback;
        private List<String> strengths;
        private List<String> improvements;
    }
}
