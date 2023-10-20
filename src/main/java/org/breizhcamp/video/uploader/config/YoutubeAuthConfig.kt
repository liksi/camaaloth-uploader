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
import java.io.IOException
import java.io.InputStreamReader
import java.security.GeneralSecurityException
import java.util.*

/**
 * Configuration file for Youtube access
 */
@Configuration
class YoutubeAuthConfig(
    @Value("\${videos.dir:./videos}/.datastore")
    private val dataStoreDir: File
) {

    @Bean
    fun jacksonFactory(): JacksonFactory {
        return JacksonFactory.getDefaultInstance()
    }

    @Bean
    @Throws(GeneralSecurityException::class, IOException::class)
    fun httpTransport(): HttpTransport {
        return GoogleNetHttpTransport.newTrustedTransport()
    }

    @Bean
    @Throws(IOException::class, GeneralSecurityException::class)
    fun ytAuthFlow(): GoogleAuthorizationCodeFlow {
        val dataStoreFactory = FileDataStoreFactory(dataStoreDir)
        val secrets = GoogleClientSecrets.load(
            jacksonFactory(),
            InputStreamReader(YoutubeAuthConfig::class.java.getResourceAsStream("/oauth-google.json"))
        )
        return GoogleAuthorizationCodeFlow.Builder(
            httpTransport(),
            jacksonFactory(),
            secrets,
            listOf(YouTubeScopes.YOUTUBE_UPLOAD, YouTubeScopes.YOUTUBE)
        )
            .setDataStoreFactory(dataStoreFactory)
            .build()
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
    @Throws(IOException::class)
    fun ytCredential(ytAuthFlow: GoogleAuthorizationCodeFlow): Credential? {
        val credential = ytAuthFlow.loadCredential("user")
        return if (credential?.getExpiresInSeconds() == null || credential.getExpiresInSeconds() < 10) {
            null
        } else credential
    }

    companion object {
        /** Id of the user for storing credential  */
        const val YT_USER_ID = "user"
    }
}
