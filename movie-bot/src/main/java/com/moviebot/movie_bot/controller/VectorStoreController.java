package com.moviebot.movie_bot.controller;

import org.springframework.web.bind.annotation.RestController;

import com.moviebot.movie_bot.dto.MovieDto;
import com.moviebot.movie_bot.service.MovieService;
import com.moviebot.movie_bot.service.TmdbService;
import com.moviebot.movie_bot.service.VectorStoreService;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

// 벡터 스토리지에 들어가는지 테스트
@RestController
public class VectorStoreController {
    private final TmdbService tmdbService;
    private final MovieService movieService;
    private final VectorStoreService vectorStoreService;

    public VectorStoreController(TmdbService tmdbService, MovieService movieService, VectorStoreService vectorStoreService) {
        this.tmdbService = tmdbService;
        this.movieService = movieService;
        this.vectorStoreService = vectorStoreService;
    }

    @GetMapping("/api/rag/add")
    public String addMovie(@RequestParam String query) {
        List<MovieDto> movies = tmdbService.searchMovie(query);

        if (movies.isEmpty()) {
            return "영화를 찾을 수 없습니다.";
        }

        for (MovieDto movie : movies) {
            movieService.saveMovieToVectorStore(movie);
        }
        
        return "Vector Store에 " + movies.size() + "개의 영화가 저장되었습니다.";
    }

    @GetMapping("/api/rag/count")
    public int count() {
        return vectorStoreService.size();
    }
    
    
}
