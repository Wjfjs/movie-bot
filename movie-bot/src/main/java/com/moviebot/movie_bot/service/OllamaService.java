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
                당신은 한국어로 영화 정보를 설명하는 AI입니다.

                [답변 규칙]
                1. 반드시 자연스러운 한국어로 답변하세요.
                2. 영어, 일본어, 중국어, 러시아어, 태국어 등 다른 언어를 사용하지 마세요.
                3. 의미 없는 외국어 문자나 이상한 단어를 생성하지 마세요.
                4. 질문에 대한 답변만 간결하게 작성하세요.
                5. 모르는 정보는 추측하지 마세요.

                [사용자 질문]
                %s
                """.formatted(question);
        
        Map<String, Object> request = Map.of(
            "model", ollamaModel,
            "prompt", prompt,
            "stream", false,
            "options", Map.of(
                    // 낮을 수록 무작위성을 줄임
                    "temperature", 0.2
            )
        );
        
        try {
            String response = WebClient.create().post()
                .uri(ollamaApiUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .block();
                
            JsonNode jsonResponse = objectMapper.readTree(response);

            return jsonResponse.path("response").asText().trim();
        } catch (Exception e) {
            e.printStackTrace();
            
            return "죄송합니다. 영화 정보를 가져오는 중 오류가 발생했습니다.";
        }
    }

    public String generateMovieAnswer(String question, String movieContext) {
        String prompt = """
            당신은 한국어 영화 정보 전문 AI입니다.

            반드시 아래 [영화 정보]에 포함된 내용만 사용하여 답변하세요.

            [답변 규칙]
            1. 기본 답변 언어는 자연스러운 한국어입니다.
            2. 장르명, 영화 제목, 배우 이름, 감독 이름, 캐릭터 이름 등 필요한 고유명사는
               한국어와 원어를 함께 표기할 수 있습니다.
            3. 외국어를 표기할 경우 반드시 "한국어(원어)" 형식을 사용하세요.
               예:
               - 슈퍼히어로(superhero)
               - 스파이더맨(Spider-Man)
               - 커스틴 던스트(Kirsten Dunst)
            4. 영화 정보에 원어가 제공되지 않은 경우 원어를 추측해서 추가하지 마세요.
            5. 영화 정보에 없는 배우 이름을 추가하지 마세요.
            6. 영화 정보에 없는 감독 이름을 추가하지 마세요.
            7. 영화 정보에 없는 등장인물을 추가하지 마세요.
            8. 영화 정보에 없는 영화 내용을 추가하지 마세요.
            9. 자신의 학습 데이터를 이용하여 영화 정보를 보충하지 마세요.
            10. 추측하거나 상상해서 답변하지 마세요.
            11. 여러 영화의 정보가 제공되더라도 서로 다른 영화의 정보를 섞지 마세요.
            12. 영화 정보에 없는 정보는 만들어내지 마세요.
            13. 줄거리 설명은 제공된 줄거리의 내용만 자연스럽게 정리하세요.
            14. 사용자가 영화 제목만 입력했다면 제목, 개봉일, 평점, 장르와 줄거리를 중심으로 설명하세요.
            15. 답변은 2~5개의 자연스러운 문장으로 작성하세요.
            16. 불필요한 외국어 문장이나 의미 없는 외국어 단어를 사용하지 마세요.
            17. 원어 표기가 필요한 경우에만 한국어(원어) 형식을 사용하세요.
            18. 배우 이름과 캐릳터 이름을 임의로 연결하지 마세요.
            19. [영화 정보]에 배우와 캐릭터의 관계가 명시되어 있지 않다면
                "배우명은 캐릭터명과 연결하여 설명하지 마세요."
            20. [영화 정보]에 있는 배우 목록은 필요할 경우
                "주요 배우는 A, B, C입니다." 형태로만 사용하세요.
            21. 배우가 특정 캐릭터를 연기했다는 정보가 [영화 정보]에 없으면
                "A가 B 역할을 맡았다"와 같은 표현을 사용하지 마세요.

            [영화 정보]
            %s

            [사용자 질문]
            %s

            [최종 답변]
            위의 [영화 정보]만 사용하여 질문에 답변하세요.
            """.formatted(movieContext, question);

        Map<String, Object> request = Map.of(
                "model", ollamaModel,
                "prompt", prompt,
                "stream", false,
                "options", Map.of(
                        "temperature", 0.2
                )
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
