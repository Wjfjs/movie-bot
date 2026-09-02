package com.moviebot.movie_bot.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.moviebot.movie_bot.rag.MovieDocument;
import com.moviebot.movie_bot.rag.SearchResult;

@Service
public class VectorStoreService {
    private final List<MovieDocument> documents = new ArrayList<>();
    private final SimilarityService similarityService;

    public VectorStoreService(SimilarityService similarityService) {
        this.similarityService = similarityService;
    }

    // 벡터에 추가
    public void addDocument(MovieDocument document) {
        documents.add(document);

        System.out.println(
                "Vector Store에 영화 추가: " + document.getTitle());
    }

    // 모두 반환
    public List<MovieDocument> getDocuments() {
        return documents;
    }

    // 사이즈 반환
    public int size() {
        return documents.size();
    }

    // 삭제
    public void clear() {
        documents.clear();
    }

    public List<SearchResult> search(float[] queryEmbedding, int topk) {
        List<SearchResult> results = new ArrayList<>();

        // 저장된 모든 영화와 질문 비교
        for (MovieDocument document : documents) {
            double similarity = similarityService.cosineSimilarity(queryEmbedding, document.getEmbedding());

            results.add(new SearchResult(document, similarity));
        }

        // 유사도가 높은순으로 정렬
        results.sort(Comparator.comparingDouble(SearchResult::getSimilarity).reversed());

        // 요청한 개수보다 결과가 적을시
        int resultSize = Math.min(topk, results.size());

        return results.subList(0, resultSize);
    }
}
