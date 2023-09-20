package org.breizhcamp.video.uploader.config;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class YoutubeConfig {

    @Bean
    public YouTube youtube(
            HttpTransport httpTransport,
            JacksonFactory jacksonFactory,
            Credential ytCredential,
            @Value("${youtube.api.root-url:https://www.googleapis.com/}") String youtubeApiRootURL
    ) {
        return new YouTube.Builder(httpTransport, jacksonFactory, ytCredential)
                .setRootUrl(youtubeApiRootURL)
                .setApplicationName("yt-uploader/1.0")
                .build();
    }
}
