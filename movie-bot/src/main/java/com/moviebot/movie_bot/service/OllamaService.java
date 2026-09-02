package com.moviebot.movie_bot.service;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

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

    public String generateMovieAnswer(
            String question,
            String movieContext
    ) {

        String prompt = """
                당신은 영화 정보를 제공하는 AI입니다.

                반드시 아래에 제공된 영화 정보를 기반으로 답변하세요.
                영화 정보에 없는 내용은 사실인 것처럼 만들어내지 마세요.

                [영화 정보]
                %s

                [사용자 질문]
                %s

                한국어로 친절하고 이해하기 쉽게 답변하세요.
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
                    .asText();

        } catch (Exception e) {

            e.printStackTrace();

            return "AI 답변을 생성하는 중 오류가 발생했습니다.";
        }
    }
}
