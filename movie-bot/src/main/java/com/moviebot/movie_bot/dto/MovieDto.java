package com.moviebot.movie_bot.dto;

public class MovieDto {
    private Long id;
    private String title;
    private String overview;
    private String releaseDate;
    private Double rating;

    public MovieDto() {
    }

    public MovieDto(
            Long id,
            String title,
            String overview,
            String releaseDate,
            Double rating
    ) {
        this.id = id;
        this.title = title;
        this.overview = overview;
        this.releaseDate = releaseDate;
        this.rating = rating;
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
}
