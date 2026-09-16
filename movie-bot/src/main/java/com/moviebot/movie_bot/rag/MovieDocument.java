package com.moviebot.movie_bot.rag;

import lombok.Getter;

@Getter
public class MovieDocument {
    private final String title;
    private final String content;
    private final float[] embedding;

    public MovieDocument(String title, String content, float[] embedding) {
        this.title = title;
        this.content = content;
        this.embedding = embedding;
    }
    
    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content; // 영화 설명
    }

    public float[] getEmbedding() {
        return embedding;   // 영화 설명을 embedding한 벡터
    }
}
