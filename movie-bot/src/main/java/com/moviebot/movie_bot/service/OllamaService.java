package com.moviebot.movie_bot.service;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

// llama3.2로 최종 답변 생성
@Service
public class OllamaService {    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${ollama.api.url}")
    private String ollamaApiUrl;

    @Value("${ollama.model}")
    private String ollamaModel;
    
    public OllamaService() {
        this.webClient = WebClient.builder().build();
        this.objectMapper = new ObjectMapper();
    }

    public String generate(String question) {
        String prompt = """
                당신은 영화 정보를 알려주는 AI입니다.

                사용자의 질문에 친절하고 정확하게 답변하세요.

                질문:
                %s
                """.formatted(question);
        
        Map<String, Object> request = Map.of(
            "model", ollamaModel,
            "prompt", prompt,
            "stream", false
        );
        
        try {
            String response = WebClient.create()
                .post()
                .uri(ollamaApiUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .block();
                
            JsonNode jsonResponse = objectMapper.readTree(response);

            return jsonResponse.path("response").asText();
        } catch (Exception e) {
            e.printStackTrace();
            
            return "죄송합니다. 영화 정보를 가져오는 중 오류가 발생했습니다.";
        }
    }

    public String generateMovieAnswer(String question, String movieContext) {
        String prompt = """
            당신은 영화 정보만 설명하는 AI입니다.

            매우 중요한 규칙입니다.

            [규칙]
            1. 반드시 [영화 정보]에 있는 내용만 사용하세요.
            2. [영화 정보]에 없는 내용은 절대로 추가하지 마세요.
            3. 영화에 대한 설명을 만들기 위해 자신의 지식을 사용하지 마세요.
            4. 추측하지 마세요.
            5. 영화 정보를 서로 섞지 마세요.
            6. 영화에 대한 정보가 여러 개 있더라도 새로운 정보를 만들어내지 마세요.
            7. 반드시 한국어로만 답변하세요.
            8. 사용자가 영화 제목만 입력했다면 해당 영화의 제목, 개봉일, 평점, 줄거리를 간단하게 설명하세요.
            9. [영화 정보]에 줄거리가 비어 있다면 줄거리를 만들어내지 마세요.
            10. [영화 정보]에 없는 감독, 배우, 원작, 제작 배경 등의 정보는 말하지 마세요.

            [영화 정보]
            %s

            [사용자 질문]
            %s

            위의 [영화 정보]만 사용하여 답변하세요.
            """.formatted(movieContext, question);

        Map<String, Object> request = Map.of(
                "model", ollamaModel,
                "prompt", prompt,
                "stream", false
        );

        try {
            String response = webClient.post()
                    .uri(ollamaApiUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode jsonNode = objectMapper.readTree(response);

            return jsonNode
                    .path("response")
                    .asText()
                    .trim();

        } 
        catch (Exception e) {
            e.printStackTrace();

            return "AI 답변을 생성하는 중 오류가 발생했습니다.";
        }
    }
}
