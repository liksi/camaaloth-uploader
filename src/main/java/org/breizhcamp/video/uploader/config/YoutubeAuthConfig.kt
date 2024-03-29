package org.breizhcamp.video.uploader.config

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.youtube.YouTubeScopes
import org.breizhcamp.video.uploader.dto.YoutubeSession
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File
import java.io.InputStreamReader

/**
 * Configuration file for Youtube access
 */
@Configuration
class YoutubeAuthConfig(
    @Value("\${videos.dir:./videos}/.datastore") private val dataStoreDir: File
) {

    @Bean
    fun jacksonFactory(): JacksonFactory {
        return JacksonFactory.getDefaultInstance()
    }

    @Bean
    fun httpTransport(): HttpTransport {
        return GoogleNetHttpTransport.newTrustedTransport()
    }

    @Bean
    fun ytAuthFlow(jacksonFactory: JacksonFactory, httpTransport: HttpTransport): GoogleAuthorizationCodeFlow {
        val secrets = GoogleClientSecrets.load(
            jacksonFactory, InputStreamReader(
                YoutubeAuthConfig::class.java.getResourceAsStream("/oauth-google.json")
                    ?: error("No oauth-google.json file")
            )
        )
        return GoogleAuthorizationCodeFlow.Builder(
            httpTransport, jacksonFactory, secrets, listOf(YouTubeScopes.YOUTUBE_UPLOAD, YouTubeScopes.YOUTUBE)
        ).setDataStoreFactory(FileDataStoreFactory(dataStoreDir)).build()
    }

    @Bean
    fun ytSession(): YoutubeSession {
        return YoutubeSession()
    }

    /**
     * Retrieve current and valid credential
     * @return Valid credential or null
     */
    @Bean
    fun ytCredential(ytAuthFlow: GoogleAuthorizationCodeFlow): Credential? = runCatching {
        checkNotNull(ytAuthFlow.loadCredential(YT_USER_ID)) { "No credential found for userId '$YT_USER_ID'" }
            .takeIf {
                it.expiresInSeconds != null && it.expiresInSeconds >= 10
            } ?: error("Credential expired")
    }
        .onFailure { println("Error loading credential: ${it.message}") }
        .getOrThrow()

    companion object {
        /** Id of the user for storing credential  */
        const val YT_USER_ID = "642242160510-fm7dpbbvt7doup56mj2agb2cfvbqvv3m.apps.googleusercontent.com"
    }
}
