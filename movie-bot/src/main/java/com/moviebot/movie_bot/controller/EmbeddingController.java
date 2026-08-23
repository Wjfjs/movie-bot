package com.moviebot.movie_bot.controller;

import org.springframework.web.bind.annotation.RestController;

import com.moviebot.movie_bot.service.EmbeddingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


// Embedding 작동 테스트
@RestController
public class EmbeddingController {
    private final EmbeddingService embeddingService;

    public EmbeddingController(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    // 벡터 차원 개수 반환
    @GetMapping("/api/embedding")
    public int createEmbedding(@RequestParam String text) {
        float[] embedding = embeddingService.createEmbedding(text);

        return embedding.length;
    }
    
}
