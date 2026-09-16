package com.moviebot.movie_bot.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.moviebot.movie_bot.rag.MovieDocument;
import com.moviebot.movie_bot.rag.SearchResult;

// 저장된 영화 검색
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

        System.out.println("[Vector Store]Vector Store에 영화 추가: " + document.getTitle());
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

    //제목으로 검색
    public List<MovieDocument> findByTitle(String title) {
        List<MovieDocument> results = new ArrayList<>();

        if (title == null || title.isBlank()) {
            return results;
        }

        String normalizedTitle = normalizeTitle(title);

        System.out.println("[Vector Store] 제목 검색: " + title);

        for (MovieDocument document : documents) {
            if (document == null || document.getTitle() == null) {
                continue;
            }

            String documentTitle = normalizeTitle(document.getTitle());

            // 제목이 정확하게 일치하는 경우
            if (documentTitle.equals(normalizedTitle)) {

                System.out.println("[Vector Store] 제목 일치: " + document.getTitle());

                results.add(document);
            }
        }

        return results;
    }

    // 제목 정규화
    private String normalizeTitle(String title) {
        if (title == null) {
            return "";
        }

        // 소문자로 변환하고 공백 제거
        return title.trim().toLowerCase().replaceAll("\\s+", "");
    }

    // 유사도 기준으로 상위 3개 저장
    // queryEmbedding 사용자의 질문 임베딩
    // topK 최대 검색 개수
    // minSimilarity 최소 유사도
    public List<SearchResult> search(float[] queryEmbedding, int topk) {
        return search(queryEmbedding, topk, 0.0);
    }

    public List<SearchResult> search(float[] queryEmbedding, int topk, double minSimilarity) {
        List<SearchResult> results = new ArrayList<>();

        System.out.println(
                "[Vector Store] 저장된 문서 수: "
                        + documents.size()
        );

        // 저장된 모든 영화와 질문 비교
        for (MovieDocument document : documents) {
            double similarity = similarityService.cosineSimilarity(queryEmbedding, document.getEmbedding());

            System.out.println("[Vector Store] " + document.getTitle() + " similarity = " + similarity);

            // 최소 유사도보다 낮으면 제외
            if (similarity >= minSimilarity) {

                System.out.println(
                        "[Vector Store] 검색 결과 추가: "
                                + document.getTitle()
                                + " / similarity = "
                                + similarity
                                + " / 기준 = "
                                + minSimilarity
                );
                
                results.add(new SearchResult(document, similarity));
            }
            else {

                System.out.println(
                        "[Vector Store] 검색 결과 제외: "
                                + document.getTitle()
                                + " / similarity = "
                                + similarity
                                + " / 기준 = "
                                + minSimilarity
                );
            }
        }

        // 유사도가 높은순으로 정렬
        results.sort(Comparator.comparingDouble(SearchResult::getSimilarity).reversed());

        int resultCount = Math.min(topk, results.size());

        System.out.println(
                "[Vector Store] 최종 검색 결과: "
                        + resultCount
                        + "개"
        );

        return results.subList(0, resultCount);
    }
}
