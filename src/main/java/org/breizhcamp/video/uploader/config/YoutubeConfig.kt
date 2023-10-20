package org.breizhcamp.video.uploader.config

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class YoutubeConfig {
    @Bean
    fun youtube(
        httpTransport: HttpTransport,
        jacksonFactory: JacksonFactory,
        ytCredential: Credential,
        @Value("\${youtube.api.root-url:https://www.googleapis.com/}") youtubeApiRootURL: String
    ): YouTube {
        return YouTube.Builder(httpTransport, jacksonFactory, ytCredential)
            .setRootUrl(youtubeApiRootURL)
            .setApplicationName("yt-uploader/1.0")
            .build()
    }
}
