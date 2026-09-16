package com.moviebot.movie_bot.rag;

import lombok.Getter;

/**
 * Vector Store 검색 결과를 저장하는 클래스
 * 어떤 영화가 검색되었는지와 질문과 얼마나 유사한지를 함께 저장
 */
@Getter
public class SearchResult {
    private final MovieDocument document;
    private final double similarity;

    public SearchResult(MovieDocument document, double similarity) {
        this.document = document;
        this.similarity = similarity;
    }

    public MovieDocument getDocument() {
        return document;    // 검색된 영화 문서
    }

    public double getSimilarity() {
        return similarity;  // 질문과 영화 문서의 유사도
    }
}
