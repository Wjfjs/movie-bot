package com.moviebot.movie_bot.discord;

import org.springframework.stereotype.Component;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.JDABuilder;
import org.springframework.beans.factory.annotation.Value;

import com.moviebot.movie_bot.service.OllamaService;
import com.moviebot.movie_bot.service.RagService;
import com.moviebot.movie_bot.service.MovieService;

@Component
public class DiscordBot extends ListenerAdapter {
    private final OllamaService ollamaService;  // 테스트용
    private final MovieService movieService;    // 테스트용
    private final RagService ragService;

    public DiscordBot(@Value("${discord.bot.token}") String token, OllamaService ollamaService, MovieService movieService, RagService ragService) throws Exception {
        this.ollamaService = ollamaService;
        this.movieService = movieService;
        this.ragService = ragService;

        JDABuilder.createDefault(token)
                .addEventListeners(this)
                .build();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        
        // 봇 자신의 메시지 무시
        if (event.getAuthor().isBot()) {
            return;
        }

        String content = event.getMessage().getContentRaw();

        System.out.println("Discord 메시지 : " + content);
        
        if (content.equals("!안녕")) {
            event.getChannel().sendMessage("안녕하세요! 저는 영화 정보 AI 봇입니다. 🎬").queue();
        }

        if (content.startsWith("!영화")) {
            String question = content.substring(3).trim();

            if (question.isEmpty()) {
                event.getChannel().sendMessage("영화에 대한 질문을 입력해주세요. 예: !영화 인셉션").queue();
                return;
            }

            // TMDB에서 영화 정보 가져오기
            //String movieContext = movieService.createMovieContext(question);    // 테스트용

            // ollama에 질문 시작 -> TMDB에서 가져온 영화 정보 context 전달
            //String answer = ollamaService.generateMovieAnswer(question, movieContext);    // 테스트용
            String answer = ragService.generateAnswer(question);

            // discord에 답변 전송
            event.getChannel().sendMessage(answer).queue();
        }
    
    }
}
