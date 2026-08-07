package com.moviebot.movie_bot.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.moviebot.movie_bot.dto.MovieDto;

@Service
public class MovieService {
    private final TmdbService tmdbService;

    public MovieService(TmdbService tmdbService) {
        this.tmdbService = tmdbService;
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
}
