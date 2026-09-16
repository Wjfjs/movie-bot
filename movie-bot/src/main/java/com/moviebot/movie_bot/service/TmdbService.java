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

    // ---------------------------------------------------------
    // 영화 검색
    // ---------------------------------------------------------
    public List<MovieDto> searchMovie(String query) {

        try {

            // 한국어 영화 검색
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

            System.out.println("TMDB 검색 응답 : " + response);

            JsonNode root = objectMapper.readTree(response);

            List<MovieDto> movies = new ArrayList<>();

            // 검색 결과에서 영화 기본 정보 추출
            for (JsonNode movie : root.path("results")) {

                Long movieId = movie.path("id").asLong();

                String title = movie.path("title").asText();

                String overview = movie.path("overview").asText();

                String releaseDate = movie.path("release_date").asText();

                Double rating = movie.path("vote_average").asDouble();

                // -------------------------------------------------
                // 영화 상세 정보
                // -------------------------------------------------

                // 한국어 상세 정보
                JsonNode koreanDetail = getMovieDetail(
                        movieId,
                        "ko-KR"
                );

                // 영어 상세 정보
                JsonNode englishDetail = getMovieDetail(
                        movieId,
                        "en-US"
                );

                // -------------------------------------------------
                // 장르
                // -------------------------------------------------

                List<String> genres = getGenres(
                        koreanDetail,
                        englishDetail
                );

                // -------------------------------------------------
                // 감독 / 배우
                // -------------------------------------------------

                // 한국어 credits
                JsonNode koreanCredits = getMovieCredits(
                        movieId,
                        "ko-KR"
                );

                // 영어 credits
                JsonNode englishCredits = getMovieCredits(
                        movieId,
                        "en-US"
                );

                String director = getDirector(
                        koreanCredits,
                        englishCredits
                );

                List<String> actors = getActors(
                        koreanCredits,
                        englishCredits
                );

                // -------------------------------------------------
                // MovieDto 생성
                // -------------------------------------------------

                MovieDto dto = new MovieDto(
                        movieId,
                        title,
                        overview,
                        releaseDate,
                        rating,
                        genres,
                        director,
                        actors
                );

                movies.add(dto);
            }

            return movies;

        } catch (Exception e) {

            e.printStackTrace();

            return List.of();
        }
    }

    // ---------------------------------------------------------
    // 영화 상세 정보 조회
    // ---------------------------------------------------------
    private JsonNode getMovieDetail(
            Long movieId,
            String language) {

        try {

            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("api.themoviedb.org")
                            .path("/3/movie/" + movieId)
                            .queryParam("language", language)
                            .build())
                    .headers(headers -> headers.setBearerAuth(apiToken))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return objectMapper.readTree(response);

        } catch (Exception e) {

            System.out.println(
                    "[TMDB] 영화 상세 정보 조회 실패: "
                            + movieId
                            + " / "
                            + language
            );

            return objectMapper.createObjectNode();
        }
    }

    // ---------------------------------------------------------
    // 장르 추출
    // ---------------------------------------------------------
    private List<String> getGenres(
            JsonNode koreanDetail,
            JsonNode englishDetail) {

        List<String> genres = new ArrayList<>();

        JsonNode koreanGenres = koreanDetail.path("genres");
        JsonNode englishGenres = englishDetail.path("genres");

        int count = Math.min(
                koreanGenres.size(),
                englishGenres.size()
        );

        for (int i = 0; i < count; i++) {

            String koreanName =
                    koreanGenres.get(i).path("name").asText();

            String englishName =
                    englishGenres.get(i).path("name").asText();

            if (!koreanName.isBlank()
                    && !englishName.isBlank()) {

                genres.add(
                        koreanName
                                + "("
                                + englishName
                                + ")"
                );

            } else if (!koreanName.isBlank()) {

                genres.add(koreanName);

            } else if (!englishName.isBlank()) {

                genres.add(englishName);
            }
        }

        return genres;
    }

    // ---------------------------------------------------------
    // 영화 출연진 / 제작진 조회
    // ---------------------------------------------------------
    private JsonNode getMovieCredits(
            Long movieId,
            String language) {

        try {

            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("api.themoviedb.org")
                            .path("/3/movie/" + movieId + "/credits")
                            .queryParam("language", language)
                            .build())
                    .headers(headers -> headers.setBearerAuth(apiToken))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return objectMapper.readTree(response);

        } catch (Exception e) {

            System.out.println(
                    "[TMDB] 출연진 정보 조회 실패: "
                            + movieId
                            + " / "
                            + language
            );

            return objectMapper.createObjectNode();
        }
    }

    // ---------------------------------------------------------
    // 감독 추출
    // ---------------------------------------------------------
    private String getDirector(
            JsonNode koreanCredits,
            JsonNode englishCredits) {

        JsonNode koreanCrew = koreanCredits.path("crew");
        JsonNode englishCrew = englishCredits.path("crew");

        String koreanDirector = "";
        String englishDirector = "";

        // 한국어 감독 이름
        for (JsonNode crew : koreanCrew) {

            if ("Director".equals(
                    crew.path("job").asText())) {

                koreanDirector = crew.path("name").asText();

                break;
            }
        }

        // 영어 감독 이름
        for (JsonNode crew : englishCrew) {

            if ("Director".equals(
                    crew.path("job").asText())) {

                englishDirector = crew.path("name").asText();

                break;
            }
        }

        return combineName(
                koreanDirector,
                englishDirector
        );
    }

    // ---------------------------------------------------------
    // 주요 배우 추출
    // ---------------------------------------------------------
    private List<String> getActors(
            JsonNode koreanCredits,
            JsonNode englishCredits) {

        List<String> actors = new ArrayList<>();

        JsonNode koreanCast = koreanCredits.path("cast");
        JsonNode englishCast = englishCredits.path("cast");

        // 최대 5명의 주요 배우
        int count = Math.min(
                5,
                Math.min(
                        koreanCast.size(),
                        englishCast.size()
                )
        );

        for (int i = 0; i < count; i++) {

            String koreanName =
                    koreanCast.get(i)
                            .path("name")
                            .asText();

            String englishName =
                    englishCast.get(i)
                            .path("name")
                            .asText();

            String combinedName =
                    combineName(
                            koreanName,
                            englishName
                    );

            if (!combinedName.isBlank()) {
                actors.add(combinedName);
            }
        }

        return actors;
    }

    // ---------------------------------------------------------
    // 한국어 이름 + 영어 이름 결합
    // 예:
    // 커스틴 던스트(Kirsten Dunst)
    // ---------------------------------------------------------
    private String combineName(
            String koreanName,
            String englishName) {

        if (koreanName == null) {
            koreanName = "";
        }

        if (englishName == null) {
            englishName = "";
        }

        koreanName = koreanName.trim();
        englishName = englishName.trim();

        // 둘 다 존재
        if (!koreanName.isBlank()
                && !englishName.isBlank()) {

            // 두 이름이 같은 경우 하나만 사용
            if (koreanName.equalsIgnoreCase(englishName)) {
                return koreanName;
            }

            return koreanName
                    + "("
                    + englishName
                    + ")";
        }

        // 한국어 이름만 존재
        if (!koreanName.isBlank()) {
            return koreanName;
        }

        // 영어 이름만 존재
        return englishName;
    }
}