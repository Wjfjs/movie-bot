package com.moviebot.movie_bot.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.moviebot.movie_bot.dto.MovieDto;
import com.moviebot.movie_bot.rag.MovieDocument;
import com.moviebot.movie_bot.rag.SearchResult;

// Discord
//    ↓
// RagService
//    ↓
// VectorStore 제목 검색
//    ↓
// VectorStore 벡터 검색
//    ↓
// TMDB 검색
//    ↓
// VectorStore 저장
//    ↓
// Ollama
//    ↓
// llama3.2
//    ↓
// Discord
@Service
public class RagService {
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final OllamaService ollamaService;
    private final MovieService movieService;

    private static final double SIMILARITY_THRESHOLD = 0.80; // 유사도 임계값

    public RagService(EmbeddingService embeddingService, VectorStoreService vectorStoreService, OllamaService ollamaService, MovieService movieService) {
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.ollamaService = ollamaService;
        this.movieService = movieService;
    }

    // VectorStore에서 질문과 관련된 영화 검색
    // 1. 제목 정확히 일치하는 영화 검색
    // 2. 없으면 Embedding 검색
    public List<SearchResult> searchMovies(String question, int topk) {

        // 제목으로 검색
        List<MovieDocument> titleMatches = vectorStoreService.findByTitle(question);

        if (!titleMatches.isEmpty()) {
            System.out.println("[RAG] 제목 검색 결과 수: " + titleMatches.size());

            List<SearchResult> results = new ArrayList<>();

            for(MovieDocument document : titleMatches) {
                if (document == null) {
                    continue;
                }

                // 제목이 정확히 일치하는 경우 유사도를 1.0으로 설정
                results.add(new SearchResult(document, 1.0));
            }

            return results.subList(0, Math.min(topk, results.size()));
        }

        // 제목 검색 결과가 없으면 Embedding 검색
        float[] queryEmbedding = embeddingService.createEmbedding(question);    // 사용자 질문을 embedding 벡터로 변환

        System.out.println(
                "[RAG] VectorStore 검색 기준 similarity = "
                        + SIMILARITY_THRESHOLD
        );

        return vectorStoreService.search(queryEmbedding, topk, SIMILARITY_THRESHOLD); // 벡터 스토어에서 가장 관련있는 영화 검색 후 반환
    }

    // VectorStore 검색 결과를 Ollama Context로 변환
    private String createContextFromResults(List<SearchResult> results) {
        if (results == null || results.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();

        for (SearchResult result : results) {
            if (result == null) {
                continue;
            }

            MovieDocument document = result.getDocument();

            if (document == null) {
                System.out.println("[RAG] 경고: MovieDocument가 null입니다.");
                continue;
            }
            
            context.append("""
                    영화 제목: %s
                    유사도: %.3f
                    영화 정보:
                    %s
                    
                    """.formatted(
                    document.getTitle(),
                    result.getSimilarity(),
                    document.getContent()
            ));
        }

        if (context.isEmpty()) {
            return "관련된 영화 정보를 찾을 수 없습니다.";
        }

        return context.toString();
    }

    // TMDB 영화 정보를 Ollama Context로 변환
    private String createContextFromMovie(MovieDto movie) {
        if (movie == null) {
            return "";
        }

        return """
                영화 제목: %s
                개봉일: %s
                평점: %.1f
                줄거리:
                %s
                """.formatted(
                movie.getTitle(),
                movie.getReleaseDate(),
                movie.getRating(),
                movie.getOverview()
        );
    }

    // 최종 영화 답변 생성
    //
    // 1. VectorStore 제목 검색
    // 2. VectorStore 벡터 검색
    // 3. 결과 있으면 Ollama
    // 4. 결과 없으면 TMDB
    // 5. TMDB에서 영화 선택
    // 6. VectorStore 저장
    // 7. Ollama
    public String generateAnswer(String question) {
        System.out.println(
                "\n========================================"
        );

        System.out.println(
                "[RAG] 사용자 질문: "
                        + question
        );

        System.out.println(
                "========================================"
        );

        // 벡터 검색============================================================================================================
        List<SearchResult> results = searchMovies(question, 3); // 질문과 관련된 영화 검색(3개)

        System.out.println("[RAG] VectorStore 검색 결과 수: " + (results != null ? results.size() : 0));

        // 검색 결과가 있을때
        if (!results.isEmpty()) {
            System.out.println("[RAG] VectorStore에서 관련 영화 발견");

            String movieContext = createContextFromResults(results); // 검색된 영화 정보를 하나의 Context 문자열로 생성

            if (movieContext.isBlank()) {
                System.out.println("[RAG] VectorStore에 실제 영화 정보가 없습니다.");
            }
            else {
                return ollamaService.generateMovieAnswer(question, movieContext);
            }
        }

        // TMDB 검색============================================================================================================
        System.out.println("[RAG] VectorStore에 관련 영화가 없습니다.");
        System.out.println("[RAG] TMDB에서 영화 검색: " + question);

        List<MovieDto> movies = movieService.searchMovies(question); // TMDB에서 영화 검색

        if (movies == null || movies.isEmpty()) {
            System.out.println("[RAG] TMDB 검색 결과 없음");

            return "관련된 영화 정보를 찾을 수 없습니다.";
        }

        System.out.println("[RAG] TMDB 검색 결과 수: " + (movies != null ? movies.size() : 0));

        // TMDB 검색 결과에서 가장 적합한 영화 선택==================================================================================
        MovieDto selectedMovie = movieService.selectBestMovie(question, movies);

        if (selectedMovie == null) {
            return "관련된 영화 정보를 찾을 수 없습니다.";
        }

        System.out.println(
                "[RAG] 선택된 영화: "
                + selectedMovie.getTitle()
        );

        // 선택된 영화 정보를 VectorStore에 저장====================================================================================
        movieService.saveMovieToVectorStore(selectedMovie);
        System.out.println("[RAG] VectorStore 저장 완료");

        // 선택된 영화 정보를 Ollama Context로 변환=================================================================================
        // String movieContext = """
        //         영화 제목: %s
        //         개봉일: %s
        //         평점: %.1f
        //         줄거리:
        //         %s
        //         """.formatted(
        //         selectedMovie.getTitle(),
        //         selectedMovie.getReleaseDate(),
        //         selectedMovie.getRating(),
        //         selectedMovie.getOverview()
        // );
        String movieContext = createContextFromMovie(selectedMovie);

        System.out.println(
                "[RAG] 최종 영화 Context 생성 완료"
        );

        System.out.println(
                "[RAG] Ollama 호출 시작"
        );

        // Ollama 호출=========================================================================================================
        String answer = ollamaService.generateMovieAnswer(question, movieContext);

        System.out.println("[RAG] Ollama 답변: " + answer);

        return answer;
    }
}
