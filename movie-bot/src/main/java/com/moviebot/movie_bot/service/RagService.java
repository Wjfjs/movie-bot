package com.moviebot.movie_bot.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.moviebot.movie_bot.rag.MovieDocument;
import com.moviebot.movie_bot.rag.SearchResult;

@Service
public class RagService {
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final OllamaService ollamaService;

    public RagService(EmbeddingService embeddingService, VectorStoreService vectorStoreService, OllamaService ollamaService) {
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.ollamaService = ollamaService;
    }

    public List<SearchResult> searchMovies(String question, int topk) {

        // 사용자 질문을 embedding 벡터로 변환
        float[] queryEmbedding = embeddingService.createEmbedding(question);

        // 벡터 스토어에서 가장 관련있는 영화 검색 후 반환
        return vectorStoreService.search(queryEmbedding, topk);
    }

    //검색된 영화 정보를 하나의 Context 문자열로 생성
    public String createContext(String question, int topk) {
        
        // 질문과 관련된 영화를 검색
        List<SearchResult> results = searchMovies(question, topk);

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

    public String generateAnswer(String question) {
        String context = createContext(question, 3); // 상위 3개의 관련 영화 정보를 가져옵니다.

        String prompt = """
            당신은 영화 정보 전문 AI입니다.

            아래에 제공된 영화 정보를 바탕으로 사용자의 질문에 답변하세요.

            [영화 정보]
            %s

            [사용자 질문]
            %s

            [답변 규칙]
            1. 제공된 영화 정보를 우선적으로 사용하세요.
            2. 영화 정보에 없는 내용은 사실인 것처럼 만들어내지 마세요.
            3. 질문과 관련된 영화만 설명하세요.
            4. 한국어로 답변하세요.
            5. 자연스럽고 이해하기 쉽게 답변하세요.

            답변:
            """.formatted(
                    context,
                    question
            );

         return ollamaService.generate(prompt);
    }
}
