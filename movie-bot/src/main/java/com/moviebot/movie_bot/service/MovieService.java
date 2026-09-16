package com.moviebot.movie_bot.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.moviebot.movie_bot.dto.MovieDto;
import com.moviebot.movie_bot.rag.MovieDocument;


// TMDB에서 영화 정보를 가져오고, VectorStore에 저장하는 서비스
// VectorStore에 없는 영화를 TMDB에서 검색하여 추가
@Service
public class MovieService {
    private final TmdbService tmdbService;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;

    public MovieService(TmdbService tmdbService, EmbeddingService embeddingService, VectorStoreService vectorStoreService) {
        this.tmdbService = tmdbService;
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
    }

    public String createMovieContext(String query) {
        List<MovieDto> movies = tmdbService.searchMovie(query);

        if (movies.isEmpty()) {
            return "검색된 영화 정보가 없습니다.";
        }

        StringBuilder context = new StringBuilder();

        for (MovieDto movie : movies) {

            context.append("""
                    
                    영화 제목: %s
                    개봉일: %s
                    평점: %.1f
                    줄거리: %s
                    
                    """.formatted(
                    movie.getTitle(),
                    movie.getReleaseDate(),
                    movie.getRating(),
                    movie.getOverview()
            ));
        }

        return context.toString();
    }

    // TMDB에서 영화 목록을 검색
    public List<MovieDto> searchMovies(String query) {
        return tmdbService.searchMovie(query);
    }

    
    public void saveMovieToVectorStore(MovieDto movie) {
        String content = """
                영화 제목: %s
                개봉일: %s
                평점: %.1f
                줄거리: %s
                """.formatted(
                movie.getTitle(),
                movie.getReleaseDate(),
                movie.getRating(),
                movie.getOverview()
        );

        float[] embedding = embeddingService.createEmbedding(content);

        MovieDocument document = new MovieDocument(movie.getTitle(), content, embedding);

        vectorStoreService.addDocument(document);
    }

    public MovieDto selectBestMovie(String query, List<MovieDto> movies) {
        if (movies == null || movies.isEmpty()) {
            return null;
        }

        // 사용자의 질문을 비교하기 위해 정규화
        String normalizedQuery = normalizeTitle(query);

        MovieDto bestMovie = null;

        // 질문과 영화 제목이 정확하게 일치하는 영화 찾기
        for (MovieDto movie : movies) {
            if (movie == null || movie.getTitle() == null) {
                continue;
            }

            String normalizedTitle = normalizeTitle(movie.getTitle());

            // 줄거리 정보가 있는 영화라면 바로 선택
            if (normalizedTitle.equals(normalizedQuery)) {
                System.out.println(
                        "[MovieService] 제목 정확히 일치: "
                                + movie.getTitle()
                );

                if (movie.getOverview() != null && !movie.getOverview().isEmpty()) {
                    return movie;
                }

                // 줄거리가 없어도 일치하는 영화는 후보로 저장
                if (bestMovie == null) {
                    bestMovie = movie;
                }
            }
        }

        // 정확히 일치하는 제목은 있지만 줄거리가 없는 경우
        if (bestMovie != null) {
            System.out.println(
                    "[MovieService] 제목 일치 영화 선택: "
                            + bestMovie.getTitle()
            );

            return bestMovie;
        }

        // 정확히 일치하는 영화가 없으면 TMDB가 반환한 첫 번째 영화 사용
        MovieDto firstMovie = movies.get(0);

        System.out.println(
                "[MovieService] 정확한 제목 일치 없음. "
                        + "TMDB 첫 번째 결과 선택: "
                        + firstMovie.getTitle()
        );

        return firstMovie;
    }

    // 영화 제목 비교용 정규화
    private String normalizeTitle(String title) {
        if (title == null) {
            return "";
        }

        return title
                .trim()
                .toLowerCase()
                .replaceAll("\\s+", "");
    }
}
