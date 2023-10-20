package org.breizhcamp.video.uploader.services

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse
import com.google.api.client.googleapis.media.MediaHttpUploader
import com.google.api.client.http.FileContent
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.*
import org.breizhcamp.video.uploader.config.YoutubeAuthConfig
import org.breizhcamp.video.uploader.controller.YoutubeCtrl
import org.breizhcamp.video.uploader.dto.Event
import org.breizhcamp.video.uploader.dto.VideoInfo
import org.breizhcamp.video.uploader.exception.UpdateException
import org.slf4j.LoggerFactory
import org.springframework.messaging.MessagingException
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import java.io.IOException
import java.math.BigDecimal
import java.math.MathContext
import java.security.GeneralSecurityException
import java.util.concurrent.BlockingDeque
import java.util.concurrent.LinkedBlockingDeque
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

/**
 * Youtube access service
 */
@Service
class YoutubeSrv(
    private val videoSrv: VideoSrv,
    private val eventSrv: EventSrv,
    private val ytAuthFlow: GoogleAuthorizationCodeFlow,
    private val template: SimpMessagingTemplate,
    private val youtube: YouTube,
    private val ytCredential: Credential?
) {
    private lateinit var uploader: YtUploader

    @PostConstruct
    fun setUp() {
        uploader = YtUploader()
        uploader.start()
    }

    @PreDestroy
    fun tearDown() {
        uploader.shutdown()
    }

    fun getAuthUrl(redirectUrl: String): String =
        ytAuthFlow
            .newAuthorizationUrl()
            .setRedirectUri(redirectUrl)
            .setAccessType("offline")
            .build()

    fun handleAuth(code: String, redirectUrl: String) {
        saveToken(
            ytAuthFlow
                .newTokenRequest(code)
                .setRedirectUri(redirectUrl)
                .execute()
        )
    }

    /**
     * Check if user is connected and has valid credentials
     *
     * @return True if connected, false otherwise
     */
    fun isConnected(): Boolean = ytCredential != null

    /**
     * Save oauth token for reuse
     *
     * @param token Token to save
     */
    fun saveToken(token: GoogleTokenResponse) {
        ytAuthFlow.createAndStoreCredential(token, YoutubeAuthConfig.YT_USER_ID)
    }

    fun getChannels(): List<Channel> = youtube
        .channels()
        .list("id,snippet")
        .setMine(true)
        .execute()
        .items

    /**
     * Retrieve all playlists for a channel id
     *
     * @param channelId Id of the channel whose playlists we want, must be accessible by the user
     * @return List of playlist
     */
    fun getPlaylists(channelId: String): List<Playlist> {
        return youtube
            .playlists()
            .list("id,snippet").setChannelId(channelId).setMaxResults(50L)
            .execute()
            .items
    }

    /**
     * Upload a video
     *
     * @param videoInfo Video to upload
     */
    @Throws(UpdateException::class)
    fun upload(videoInfo: VideoInfo) {
        uploader.uploadVideo(videoInfo)
    }

    /**
     * @return waiting video to upload
     */
    fun listWaiting(): List<VideoInfo> {
        return uploader.listWaiting()
    }

    /**
     * Youtube uploader thread
     */
    private inner class YtUploader : Thread("YtUploader") {
        /**
         * List of video to upload
         */
        private val videoToUpload: BlockingDeque<VideoInfo> = LinkedBlockingDeque()
        private var running = true

        @Throws(UpdateException::class)
        fun uploadVideo(videoInfo: VideoInfo) {
            videoToUpload.addLast(videoInfo)
            videoInfo.status = VideoInfo.Status.WAITING
            updateVideo(videoInfo)
        }

        override fun run() {
            var lastUpload: String? = null
            var nbErrors = 0
            try {
                while (running) {
                    val videoInfo = videoToUpload.take()
                    logger.info("Uploading video: [{}]", videoInfo.path)
                    try {
                        lastUpload = videoInfo.dirName
                        val event = eventSrv.readAndGetById(videoInfo.eventId!!)
                        var speakers = event!!.speakers
                        if (speakers!!.endsWith(", ")) speakers = speakers.substring(0, speakers.length - 2)
                        val video = Video()
                        val videoStatus = VideoStatus()
                        videoStatus.setPrivacyStatus("unlisted")
                        video.setStatus(videoStatus)
                        val snippet = VideoSnippet()
                        video.setSnippet(snippet)
                        snippet.setTitle(makeTitle(event, speakers))
                        //youtube doesn't support formatting, we keep the markdown as it readable as is
                        if (event.description != null) {
                            snippet.setDescription(event.description!!.replace('<', '〈').replace('>', '〉'))
                        }
                        val videoContent = FileContent("video/*", videoInfo.path!!.toFile())
                        val insert = youtube.videos().insert("snippet,status", video, videoContent)
                        val uploader = insert.mediaHttpUploader
                        uploader.setChunkSize(1024 * 1024 * 50) //10MB in order to have progress info often :p
                        uploader.setProgressListener { httpUploader: MediaHttpUploader ->
                            try {
                                when (httpUploader.uploadState) {
                                    MediaHttpUploader.UploadState.NOT_STARTED -> logger.info(
                                        "[{}] Not started",
                                        videoInfo.eventId
                                    )

                                    MediaHttpUploader.UploadState.INITIATION_STARTED -> {
                                        logger.info("[{}] Init started", videoInfo.eventId)
                                        videoInfo.status = VideoInfo.Status.INITIALIZING
                                        updateVideo(videoInfo)
                                    }

                                    MediaHttpUploader.UploadState.INITIATION_COMPLETE -> {
                                        logger.info("[{}] Init complete", videoInfo.eventId)
                                        videoInfo.status = VideoInfo.Status.IN_PROGRESS
                                        videoInfo.progression = BigDecimal.ZERO
                                        updateVideo(videoInfo)
                                    }

                                    MediaHttpUploader.UploadState.MEDIA_IN_PROGRESS -> {
                                        val progress = httpUploader.getProgress()
                                        logger.info("[{}] Upload in progress: [{}]", videoInfo.eventId, progress)
                                        val percent = BigDecimal(progress * 100, MathContext(3))
                                        videoInfo.status = VideoInfo.Status.IN_PROGRESS
                                        videoInfo.progression = percent
                                        updateVideo(videoInfo)
                                    }

                                    MediaHttpUploader.UploadState.MEDIA_COMPLETE -> logger.info(
                                        "[{}] Upload video file complete",
                                        videoInfo.eventId
                                    )
                                }
                            } catch (e: UpdateException) {
                                //not a critical exception, let the upload continue
                                logger.warn("Cannot send or write update for video [{}]", videoInfo.dirName, e)
                            }
                        }

                        //this call is blocking until video is completely uploaded
                        val insertedVideo = insert.execute()
                        videoInfo.youtubeId = insertedVideo.id
                        videoInfo.progression = null

                        //upload thumbnail if available
                        if (videoInfo.thumbnail != null) {
                            videoInfo.status = VideoInfo.Status.THUMBNAIL
                            updateVideo(videoInfo)
                            uploadThumbnail(videoInfo)
                        }
                        insertInPlaylist(videoInfo)
                        videoInfo.status = VideoInfo.Status.DONE
                        updateVideo(videoInfo)
                        logger.info("[{}] Video uploaded, end of process", videoInfo.eventId)
                        nbErrors = 0
                    } catch (e: UpdateException) {
                        logger.error("Error when uploading [{}]", lastUpload, e)
                        videoInfo.status = VideoInfo.Status.FAILED
                        try {
                            updateVideo(videoInfo)
                        } catch (ex: UpdateException) {
                            logger.error("Unable to update metadata", ex)
                        }
                        nbErrors++
                        if (nbErrors > 5) {
                            throw RuntimeException("At least 5 videos failed to upload, stopping thread")
                        }
                    } catch (e: GeneralSecurityException) {
                        logger.error("Error when uploading [{}]", lastUpload, e)
                        videoInfo.status = VideoInfo.Status.FAILED
                        try {
                            updateVideo(videoInfo)
                        } catch (ex: UpdateException) {
                            logger.error("Unable to update metadata", ex)
                        }
                        nbErrors++
                        if (nbErrors > 5) {
                            throw RuntimeException("At least 5 videos failed to upload, stopping thread")
                        }
                    } catch (e: IOException) {
                        logger.error("Error when uploading [{}]", lastUpload, e)
                        videoInfo.status = VideoInfo.Status.FAILED
                        try {
                            updateVideo(videoInfo)
                        } catch (ex: UpdateException) {
                            logger.error("Unable to update metadata", ex)
                        }
                        nbErrors++
                        if (nbErrors > 5) {
                            throw RuntimeException("At least 5 videos failed to upload, stopping thread")
                        }
                    }
                }
            } catch (e: InterruptedException) {
                running = false
            }
        }

        @Throws(GeneralSecurityException::class, IOException::class)
        private fun insertInPlaylist(videoInfo: VideoInfo) {
            val playlistId = videoInfo.playlistId ?: return
            logger.info("[{}] Setting video in playlist [{}]", videoInfo.eventId, playlistId)
            val item = PlaylistItem()
            val snippet = PlaylistItemSnippet()
            item.setSnippet(snippet)
            snippet.setPlaylistId(playlistId)
            val resourceId = ResourceId()
            resourceId.setVideoId(videoInfo.youtubeId)
            resourceId.setKind("youtube#video")
            snippet.setResourceId(resourceId)
            youtube.playlistItems().insert("snippet,status", item).execute()
            logger.info("[{}] Video set in playlist", videoInfo.eventId)
        }

        @Throws(GeneralSecurityException::class, IOException::class)
        private fun uploadThumbnail(videoInfo: VideoInfo) {
            logger.info("[{}] Uploading and defining thumbnail [{}]", videoInfo.eventId, videoInfo.thumbnail)
            val thumb = FileContent("image/png", videoInfo.thumbnail!!.toFile())
            youtube.thumbnails().set(videoInfo.youtubeId, thumb).execute()
            logger.info("[{}] Thumbnail set", videoInfo.eventId)
        }

        fun shutdown() {
            running = false
        }

        fun listWaiting(): List<VideoInfo> {
            return ArrayList(videoToUpload)
        }

        @Throws(UpdateException::class)
        private fun updateVideo(video: VideoInfo) {
            try {
                template.convertAndSend(YoutubeCtrl.VIDEOS_TOPIC, video)
                videoSrv.updateVideo(video)
            } catch (e: MessagingException) {
                throw UpdateException(e)
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(YoutubeSrv::class.java)

        /**
         * Make a title compatible with Youtube : 100 chars with no < or >.
         * https://developers.google.com/youtube/v3/docs/videos#snippet.title
         *
         * @param event    Event detail
         * @param speakers Speakers' name
         * @return Compatible twitter video title
         */
        private fun makeTitle(event: Event, speakers: String): String {
            var name = "[REFACTO] " + event.name
            if (name.length + speakers.length + 3 > 100) {
                name = name.substring(0, 100 - speakers.length - 4) + "…"
            }
            name = name.replace('<', '〈').replace('>', '〉')
            return "$name ($speakers)"
        }
    }
}
