package com.moviebot.movie_bot.dto;

import java.util.List;

public class MovieDto {

    // TMDB 영화 ID
    private Long id;

    // 영화 제목
    private String title;

    // 영화 줄거리
    private String overview;

    // 개봉일
    private String releaseDate;

    // TMDB 평점
    private Double rating;

    // 영화 장르
    private List<String> genres;

    // 감독
    private String director;

    // 주요 배우
    private List<String> actors;

    public MovieDto() {
    }

    public MovieDto(
            Long id,
            String title,
            String overview,
            String releaseDate,
            Double rating,
            List<String> genres,
            String director,
            List<String> actors
    ) {
        this.id = id;
        this.title = title;
        this.overview = overview;
        this.releaseDate = releaseDate;
        this.rating = rating;
        this.genres = genres;
        this.director = director;
        this.actors = actors;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getOverview() {
        return overview;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public Double getRating() {
        return rating;
    }

    public List<String> getGenres() {
        return genres;
    }

    public String getDirector() {
        return director;
    }

    public List<String> getActors() {
        return actors;
    }
}