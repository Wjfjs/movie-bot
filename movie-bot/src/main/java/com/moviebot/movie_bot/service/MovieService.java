package com.moviebot.movie_bot.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.moviebot.movie_bot.dto.MovieDto;
import com.moviebot.movie_bot.rag.MovieDocument;

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
