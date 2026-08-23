package com.moviebot.movie_bot.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;  // HTTP의 데이터 형식(Content-Type / Accept)을 표현하는 클래스 (JSON인지, XML인지, HTML인지 알려주는 역할)
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

// nomic-embed-text 사용
@Service
public class EmbeddingService {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${ollama.embedding.url}")
    private String embeddingUrl;

    @Value("${ollama.embedding.model}")
    private String embeddingModel;

    public EmbeddingService() {
        this.webClient = WebClient.builder().build();
        this.objectMapper = new ObjectMapper();
    }

    // 예: [0.021f, -0.134f, 0.552f, ...] 벡터형식으로 반환
    public float[] createEmbedding(String text) {
        Map<String, Object> request = Map.of(
            "model", embeddingModel,
            "prompt", text
        );

        try {
            String response = webClient.post()
                .uri(embeddingUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            JsonNode jsonNode = objectMapper.readTree(response);
            JsonNode embeddingNode = jsonNode.path("embedding");
            float[] embedding = new float[embeddingNode.size()];

            for (int i = 0; i < embeddingNode.size(); i++) {
                embedding[i] = (float) embeddingNode.get(i).asDouble();
            }

            return embedding;
        }
        catch (Exception e) {
            throw new RuntimeException("Embedding 생성 중 오류가 발생했습니다.", e);
        }
    }
}
