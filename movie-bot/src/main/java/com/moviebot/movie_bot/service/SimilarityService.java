package com.moviebot.movie_bot.service;

import org.springframework.stereotype.Service;

// 벡터간 유사도 비교 1에 가까울 수록 비슷함 (-1 ~ 1)
@Service
public class SimilarityService {
    public double cosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA.length != vectorB.length) {
            throw new IllegalArgumentException("벡터의 크기가 다릅니다.");
        }

        double dotProduct = 0.0;
        double magnitudeA = 0.0;
        double magnitudeB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];  // 두 벡터의 내적
            magnitudeA += vectorA[i] * vectorA[i];  // 벡터 A의 크기 계산을 위한 값
            magnitudeB += vectorB[i] * vectorB[i];  // 벡터 B의 크기 계산을 위한 값
        }

        // 벡터 크기 계산
        magnitudeA = Math.sqrt(magnitudeA);
        magnitudeB = Math.sqrt(magnitudeB);

        // 0으로 나누는 상황 방지용
        if (magnitudeA == 0 || magnitudeB == 0) {
            return 0.0;
        }

        return dotProduct / (magnitudeA * magnitudeB);
    }
}
