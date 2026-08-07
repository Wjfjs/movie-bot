package com.moviebot.movie_bot.service;

import com.moviebot.movie_bot.dto.MovieDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Service
public class TmdbService {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${tmdb.api.token}")
    private String apiToken;

    @Value("${tmdb.api.url}")
    private String apiUrl;

    public TmdbService() {
        this.webClient = WebClient.builder().build();
        this.objectMapper = new ObjectMapper();
    }

    public List<MovieDto> searchMovie(String query) {

        try {
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("api.themoviedb.org")
                            .path("/3/search/movie")
                            .queryParam("query", query)
                            .queryParam("language", "ko-KR")
                            .queryParam("region", "KR")
                            .build())
                    .headers(headers -> headers.setBearerAuth(apiToken))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            System.out.println("TMDB 응답 : " + response);
            
            JsonNode root = objectMapper.readTree(response);

            List<MovieDto> movies = new ArrayList<>();

            for (JsonNode movie : root.path("results")) {
                MovieDto dto = new MovieDto(
                        movie.path("id").asLong(),
                        movie.path("title").asText(),
                        movie.path("overview").asText(),
                        movie.path("release_date").asText(),
                        movie.path("vote_average").asDouble()
                );
                movies.add(dto);
            }

            return movies;
        }
        catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }
}
