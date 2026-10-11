
package com.moviebot.movie_bot.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.moviebot.movie_bot.rag.MovieDocument;
import com.moviebot.movie_bot.rag.SearchResult;

// PostgreSQL 기반 영화 벡터 저장소
@Service
public class VectorStoreService {

    private static final String TABLE_NAME = "moviebot.movie_documents";
    private static final int EMBEDDING_DIMENSION = 768;

    private final JdbcTemplate jdbcTemplate;

    public VectorStoreService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // float[] 임베딩을 PostgreSQL vector 형식으로 변환
    private String toVectorString(float[] embedding) {
        if (embedding == null) {
            throw new IllegalArgumentException("임베딩이 null입니다.");
        }

        if (embedding.length != EMBEDDING_DIMENSION) {
            throw new IllegalArgumentException(
                    "임베딩 차원 오류: 기대값 = "
                            + EMBEDDING_DIMENSION
                            + ", 실제값 = "
                            + embedding.length);
        }

        return java.util.Arrays.toString(embedding);
    }

    // PostgreSQL vector 문자열을 float[]로 변환
    private float[] parseEmbedding(String vectorText) {
        if (vectorText == null || vectorText.length() < 2) {
            throw new IllegalArgumentException("잘못된 vector 데이터입니다.");
        }

        String values = vectorText.substring(1, vectorText.length() - 1);

        if (values.isBlank()) {
            return new float[0];
        }

        String[] parts = values.split(",");
        float[] embedding = new float[parts.length];

        for (int i = 0; i < parts.length; i++) {
            embedding[i] = Float.parseFloat(parts[i].trim());
        }

        return embedding;
    }

    // ResultSet을 MovieDocument로 변환
    private MovieDocument mapDocument(ResultSet rs, int rowNum) throws SQLException {
        String title = rs.getString("title");
        String content = rs.getString("content");
        String vectorText = rs.getString("embedding");

        return new MovieDocument(
                title,
                content,
                parseEmbedding(vectorText));
    }

    // 영화 데이터 저장
    public void addDocument(MovieDocument document) {
        // 1. 이미 같은 제목의 영화가 저장되어 있는지 확인
        List<MovieDocument> existingDocuments = findByTitle(document.getTitle());

        if (!existingDocuments.isEmpty()) {
            System.out.println("[Vector Store] 중복 영화 저장 생략: " + document.getTitle());
            return;
        }

        String sql = """
                INSERT INTO moviebot.movie_documents
                    (title, content, embedding)
                VALUES (?, ?, ?::vector)
                """;

        String vector = toVectorString(document.getEmbedding());

        jdbcTemplate.update(
                sql,
                document.getTitle(),
                document.getContent(),
                vector);

        System.out.println("[Vector Store] PostgreSQL에 영화 저장: " + document.getTitle());
    }

    // 저장된 모든 영화 조회(할당 안됨)
    public List<MovieDocument> getDocuments() {
        String sql = """
                SELECT title, content, embedding::text AS embedding
                FROM moviebot.movie_documents
                ORDER BY id
                """;

        return jdbcTemplate.query(sql, this::mapDocument);
    }

    // 저장된 영화 개수 조회(할당 안됨)
    public int size() {
        String sql = "SELECT COUNT(*) FROM moviebot.movie_documents";

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);

        return count == null ? 0 : count;
    }

    // 모든 영화 데이터 삭제(할당 안됨)
    public void clear() {
        jdbcTemplate.update("DELETE FROM moviebot.movie_documents");

        System.out.println("[Vector Store] PostgreSQL 영화 데이터 전체 삭제");
    }

    // 제목으로 검색(대소문자 및 공백 무시)
    public List<MovieDocument> findByTitle(String title) {
        List<MovieDocument> results = new ArrayList<>();

        if (title == null || title.isBlank()) {
            return results;
        }

        String normalizedTitle = normalizeTitle(title);

        String sql = """
                SELECT title, content, embedding::text AS embedding
                FROM moviebot.movie_documents
                WHERE regexp_replace(
                    lower(title), '[[:space:]]+', '', 'g'
                ) = ?
                ORDER BY id
                """;

        results.addAll(jdbcTemplate.query(sql, this::mapDocument, normalizedTitle));

        System.out.println("[Vector Store] 제목 검색: " + title + " / 결과: " + results.size() + "개");

        return results;
    }

    // 제목 정규화
    private String normalizeTitle(String title) {
        if (title == null) {
            return "";
        }

        return title.trim()
                .toLowerCase()
                .replaceAll("\\s+", "");
    }

    // 기본 유사도 검색
    public List<SearchResult> search(float[] queryEmbedding, int topk) {
        return search(queryEmbedding, topk, 0.0);
    }

    // PostgreSQL pgvector를 이용한 유사도 검색
    public List<SearchResult> search(
            float[] queryEmbedding,
            int topk,
            double minSimilarity) {

        if (queryEmbedding == null
                || queryEmbedding.length != EMBEDDING_DIMENSION) {
            throw new IllegalArgumentException(
                    "검색 임베딩은 "
                            + EMBEDDING_DIMENSION
                            + "차원이어야 합니다.");
        }

        if (topk <= 0) {
            return new ArrayList<>();
        }

        String vector = toVectorString(queryEmbedding);

        String sql = """
                SELECT
                    title,
                    content,
                    embedding::text AS embedding,
                    1 - (embedding <=> ?::vector) AS similarity
                FROM moviebot.movie_documents
                WHERE 1 - (embedding <=> ?::vector) >= ?
                ORDER BY embedding <=> ?::vector ASC
                LIMIT ?
                """;

        List<SearchResult> results = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new SearchResult(
                        mapDocument(rs, rowNum),
                        rs.getDouble("similarity")),
                vector,
                vector,
                minSimilarity,
                vector,
                topk);

        System.out.println("[Vector Store] PostgreSQL 검색 결과: " + results.size() + "개 / 기준 유사도: " + minSimilarity);

        for (SearchResult result : results) {
            System.out.println("[Vector Store] " + result.getDocument().getTitle() + " / similarity = " + result.getSimilarity());
        }

        return results;
    }
}
