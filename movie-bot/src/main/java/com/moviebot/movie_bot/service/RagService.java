package com.moviebot.movie_bot.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.moviebot.movie_bot.dto.MovieDto;
import com.moviebot.movie_bot.rag.MovieDocument;
import com.moviebot.movie_bot.rag.SearchResult;

// RagService
//        ↓
// VectorStore 검색
//        ↓
// 필요하면 TMDB 검색
//        ↓
// VectorStore 저장
//        ↓
// createContextFromResults()
//        ↓
// OllamaService
//        ↓
// llama3.2
//        ↓
// Discord
@Service
public class RagService {
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final OllamaService ollamaService;
    private final MovieService movieService;

    public RagService(EmbeddingService embeddingService, VectorStoreService vectorStoreService, OllamaService ollamaService, MovieService movieService) {
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.ollamaService = ollamaService;
        this.movieService = movieService;
    }

    // VectorStore에서 질문과 관련된 영화를 검색
    public List<SearchResult> searchMovies(String question, int topk) {

        float[] queryEmbedding = embeddingService.createEmbedding(question);    // 사용자 질문을 embedding 벡터로 변환

        return vectorStoreService.search(queryEmbedding, topk); // 벡터 스토어에서 가장 관련있는 영화 검색 후 반환
    }

    private String createContextFromResults(List<SearchResult> results) {
        if (results.isEmpty()) {
            return "관련된 영화 정보를 찾을 수 없습니다.";
        }

        StringBuilder context = new StringBuilder();
        context.append("다음은 질문과 관련된 영화 정보입니다.\n\n");

        for (SearchResult result : results) {
            MovieDocument document = result.getDocument();
            context.append("영화 제목: ").append(document.getTitle()).append("\n");
            context.append(document.getContent()).append("\n");
            context.append("검색 유사도: ").append(String.format("%.4f", result.getSimilarity())).append("\n\n");
        }

        return context.toString();
    }

    /** 
     * 1. VectorStore 검색
     * 2. 검색 결과가 없으면 TMDB 검색
     * 3. TMDB에서 찾은 영화를 VectorStore에 저장
     * 4. 다시 VectorStore 검색
     * 5. 검색 결과를 llama3.2에 전달
     */
    public String generateAnswer(String question) {
        List<SearchResult> results = searchMovies(question, 3); // 질문과 관련된 영화 검색

        if (results.isEmpty()) {
            System.out.println("[RAG] VectorStore에 관련 영화가 없습니다.");

            System.out.println("[RAG] TMDB에서 영화 검색: " + question);

            List<MovieDto> movies = movieService.searchMovies(question); // TMDB에서 영화 검색

            if (movies.isEmpty()) {
                return "관련된 영화 정보를 찾을 수 없습니다.";
            }

            int saveCount = Math.min(3, movies.size()); // 상위 3개만 저장
            for (int i = 0; i < saveCount; i++) { // TMDB에서 찾은 영화를 상위 3개만 VectorStore에 저장
                MovieDto movie = movies.get(i);

                System.out.println("[RAG] VectorStore에 영화 저장: " + movie.getTitle());

                movieService.saveMovieToVectorStore(movie);
            }

            results = searchMovies(question, 3); // 다시 VectorStore에서 영화 검색
        }

        String movieContext = createContextFromResults(results); // 검색된 영화 정보를 하나의 Context 문자열로 생성

        return ollamaService.generateMovieAnswer(question, movieContext);
    }
}
