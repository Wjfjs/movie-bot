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
}
