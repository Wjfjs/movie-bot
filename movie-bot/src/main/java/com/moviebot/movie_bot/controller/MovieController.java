package com.moviebot.movie_bot.controller;

import org.springframework.web.bind.annotation.RestController;

import com.moviebot.movie_bot.dto.MovieDto;
import com.moviebot.movie_bot.service.OllamaService;
import com.moviebot.movie_bot.service.TmdbService;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
public class MovieController {
    private final OllamaService ollamaService;
    private final TmdbService tmdbService;
    
    public MovieController(OllamaService ollamaService, TmdbService tmdbService) {
        this.ollamaService = ollamaService;
        this.tmdbService = tmdbService;
    }

    @GetMapping("/generate-response")
    public String generateResponse(@RequestParam String send) {
        return ollamaService.generate(send);
    }

    @GetMapping("/api/movie/search")
    public List<MovieDto> searchMovie(@RequestParam String query) {
        return tmdbService.searchMovie(query);
    }
}
