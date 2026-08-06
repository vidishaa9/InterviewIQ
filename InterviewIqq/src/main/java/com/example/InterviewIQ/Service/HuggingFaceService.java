package com.example.InterviewIQ.Service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;


@Service
@Slf4j
public class HuggingFaceService {


    @Value("${app.huggingface.api-key}")
    private String apiKey;


    @Value("${app.huggingface.api-url}")
    private String apiUrl;



    private final RestTemplate restTemplate =
            new RestTemplate();


    private final ObjectMapper objectMapper =
            new ObjectMapper();



    // ======================================
    // Resume Analysis
    // ======================================

    public SkillExtractionResult extractSkills(String resumeText){


        String prompt = """
                You are a resume analyzer.

                Extract details from this resume.

                Return ONLY JSON:

                {
                 "skills":[],
                 "experience_level":"",
                 "job_roles":[],
                 "summary":"",
                 "years_of_experience":0
                }


                Resume:

                """ + resumeText;



        String response = callAI(prompt);



        try{


            JsonNode root =
                    objectMapper.readTree(
                            clean(response)
                    );


            return SkillExtractionResult.builder()

                    .skills(
                            parseArray(root.get("skills"))
                    )

                    .experienceLevel(
                            root.path(
                                    "experience_level"
                            ).asText("mid")
                    )

                    .jobRoles(
                            parseArray(
                                    root.get("job_roles")
                            )
                    )


                    .summary(
                            root.path(
                                    "summary"
                            ).asText("")
                    )


                    .yearsOfExperience(
                            root.path(
                                    "years_of_experience"
                            ).asInt(0)
                    )

                    .build();



        }catch(Exception e){

            log.error(
                    "Resume parsing failed",
                    e
            );


            return new SkillExtractionResult(
                    List.of("Unknown"),
                    "mid",
                    List.of("Developer"),
                    "Processed",
                    0
            );

        }


    }





    // ======================================
    // Generate Interview Questions
    // ======================================


    public List<GeneratedQuestion> generateQuestions(
            List<String> skills,
            String experience,
            String role
    ){


        String prompt = """
                
                You are an interviewer.

                Generate 8 interview questions.

                Skills:
                %s

                Experience:
                %s

                Role:
                %s


                Return ONLY JSON:

                {
                 "questions":[
                  {
                   "text":"",
                   "category":"",
                   "difficulty":""
                  }
                 ]
                }

                """.formatted(
                String.join(",",skills),
                experience,
                role
        );



        try{


            JsonNode root =
                    objectMapper.readTree(
                            clean(callAI(prompt))
                    );


            return objectMapper
                    .readerForListOf(
                            GeneratedQuestion.class
                    )
                    .readValue(
                            root.get("questions")
                    );



        }catch(Exception e){

            log.error(
                    "Question generation failed",
                    e
            );

            return List.of(

                    new GeneratedQuestion(
                            "Explain OOP concepts",
                            "TECHNICAL",
                            "EASY"
                    ),

                    new GeneratedQuestion(
                            "Explain your project",
                            "BEHAVIORAL",
                            "MEDIUM"
                    )

            );

        }

    }





    // ======================================
    // Evaluate Answer
    // ======================================


    public AnswerEvaluation evaluateAnswer(
            String question,
            String answer,
            String category
    ){


        String prompt="""

                You are an interview coach.

                Question:
                %s


                Candidate Answer:
                %s


                Return ONLY JSON:


                {
                 "score":0,
                 "feedback":"",
                 "strengths":[],
                 "improvements":[]
                }

                """.formatted(
                question,
                answer
        );



        try{


            JsonNode root =
                    objectMapper.readTree(
                            clean(
                                    callAI(prompt)
                            )
                    );


            return AnswerEvaluation.builder()

                    .score(
                            root.path(
                                    "score"
                            ).asDouble(5)
                    )

                    .feedback(
                            root.path(
                                    "feedback"
                            ).asText()
                    )

                    .strengths(
                            parseArray(
                                    root.get("strengths")
                            )
                    )

                    .improvements(
                            parseArray(
                                    root.get("improvements")
                            )
                    )

                    .build();



        }catch(Exception e){


            return AnswerEvaluation.builder()

                    .score(5.0)

                    .feedback(
                            "Keep improving your answer"
                    )

                    .strengths(
                            List.of(
                                    "Attempted answer"
                            )
                    )

                    .improvements(
                            List.of(
                                    "Add more examples"
                            )
                    )

                    .build();

        }

    }





    // ======================================
    // HuggingFace API CALL
    // ======================================


    private String callAI(String prompt){



        HttpHeaders headers =
                new HttpHeaders();


        headers.setContentType(
                MediaType.APPLICATION_JSON
        );


        headers.setBearerAuth(
                apiKey
        );




        Map<String,Object> body =
                Map.of(

                        "inputs",
                        prompt,


                        "parameters",
                        Map.of(
                                "max_new_tokens",
                                1024,

                                "temperature",
                                0.3
                        )

                );



        HttpEntity<Map<String,Object>> request =
                new HttpEntity<>(
                        body,
                        headers
                );



        try{


            ResponseEntity<String> response =
                    restTemplate.postForEntity(
                            apiUrl,
                            request,
                            String.class
                    );



            JsonNode root =
                    objectMapper.readTree(
                            response.getBody()
                    );


            return root
                    .get(0)
                    .get("generated_text")
                    .asText();



        }catch(Exception e){


            log.error(
                    "HuggingFace failed {}",
                    e.getMessage()
            );


            throw new RuntimeException(
                    "AI unavailable"
            );

        }


    }






    private String clean(String text){

        return text
                .replace("```json","")
                .replace("```","")
                .trim();

    }



    private List<String> parseArray(JsonNode node){

        if(node==null || !node.isArray())
            return List.of();


        return objectMapper.convertValue(
                node,
                List.class
        );

    }







    // DTOs


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class SkillExtractionResult{

        private List<String> skills;

        private String experienceLevel;

        private List<String> jobRoles;

        private String summary;

        private Integer yearsOfExperience;

    }



    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class GeneratedQuestion{

        private String text;

        private String category;

        private String difficulty;

    }



    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class AnswerEvaluation{

        private Double score;

        private String feedback;

        private List<String> strengths;

        private List<String> improvements;

    }


}