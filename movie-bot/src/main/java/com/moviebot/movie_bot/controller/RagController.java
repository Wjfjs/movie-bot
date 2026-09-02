package com.moviebot.movie_bot.controller;

import org.springframework.web.bind.annotation.RestController;

import com.moviebot.movie_bot.rag.SearchResult;
import com.moviebot.movie_bot.service.RagService;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

// 사용자 질문
//       ↓
// nomic-embed-text
//       ↓
// 질문 벡터
//       ↓
// Vector Store
//       ↓
// Cosine Similarity
//       ↓
// 관련 영화 3개
//       ↓
// Context
//       ↓
// llama3.2
//       ↓
// 답변

// RAG 검색 테스트
@RestController
public class RagController {
    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    // RAG 검색 테스트
    @GetMapping("/api/rag/search")
    public List<SearchResult> search(@RequestParam String question) {
        return ragService.searchMovies(question, 3);
    }

    // RAG + llama3.2 최종 답변 테스트
    @GetMapping("/api/rag/answer")
    public String answer(@RequestParam String question) {
        return ragService.generateAnswer(question);
    }
    
}
