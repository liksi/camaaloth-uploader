package org.breizhcamp.video.uploader.controller

import org.breizhcamp.video.uploader.dto.VideoInfo
import org.breizhcamp.video.uploader.dto.YoutubeSession
import org.breizhcamp.video.uploader.exception.UpdateException
import org.breizhcamp.video.uploader.services.*
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.annotation.SubscribeMapping
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.io.IOException
import java.security.GeneralSecurityException
import java.util.function.Function
import java.util.stream.Collectors

/**
 * Controller to handle Google authentication
 */
@Controller
@RequestMapping("/yt")
class YoutubeCtrl(
    private val youtubeSrv: YoutubeSrv,
    private val fileSrv: FileSrv,
    private val videoSrv: VideoSrv,
    private val ytSession: YoutubeSession
) {
    private var redirectUrl: String? = null

    @GetMapping("/auth")
    fun auth(@RequestParam baseUrl: String): String {
        redirectUrl = "${baseUrl}yt/return"
        return "redirect:" + youtubeSrv.getAuthUrl(redirectUrl!!)
    }

    @GetMapping("/return")
    @Throws(IOException::class, GeneralSecurityException::class)
    fun returnAuth(@RequestParam code: String): String {
        youtubeSrv.handleAuth(code, redirectUrl!!)
        reloadYtSession()
        return "redirect:/"
    }

    @GetMapping("/reload")
    @Throws(GeneralSecurityException::class, IOException::class)
    fun reloadYtSession(): String {
        val channels = youtubeSrv.getChannels()
        ytSession.channels = channels
        if (channels.size == 1) {
            ytSession.apply {
                curChan = ytSession.channels!![0]
                playlists = youtubeSrv.getPlaylists(ytSession.curChan!!.id)
            }
        }
        return "redirect:/"
    }

    @PostMapping("/curPlaylist")
    fun changeCurPlaylist(@RequestParam playlist: String): String {
        if ("none" == playlist) {
            ytSession.curPlaylist = null
        } else if (ytSession.playlists != null) {
            ytSession.playlists
                ?.firstOrNull { it.id == playlist }
                ?.run { ytSession.curPlaylist = this }
        }
        return "redirect:/"
    }

    @PostMapping("uploadAll")
    @Throws(UpdateException::class)
    fun uploadAll(): String {
        videoSrv.list()
            .filter { it.status === VideoInfo.Status.NOT_STARTED }
            .forEach { videoInfo ->
                ytSession.curPlaylist?.let { videoInfo.playlistId = it.id }
                youtubeSrv.upload(videoInfo)
            }
        return "redirect:/"
    }

    @SubscribeMapping(VIDEOS_TOPIC)
    fun subscribe(): Collection<VideoInfo> {
        val videoById = videoSrv.list()
            .filter { it.eventId != null }
            .associateBy { it.eventId!! }

        youtubeSrv.listWaiting()
            .map { it.eventId }
            .forEach { id -> videoById[id]?.status = VideoInfo.Status.WAITING }

        return videoById.values
            .sortedBy { it.dirName }
    }

    @MessageMapping("$VIDEOS_TOPIC/upload")
    fun upload(@Payload path: String) {
        PathUtils.getIdFromPath(path)?.let {
            videoSrv.readDir(fileSrv.recordingDir.resolve(path))?.let {
                ytSession.curPlaylist?.let { currentPlaylist ->
                    it.playlistId = currentPlaylist.id
                }
                youtubeSrv.upload(it)
            }
        }
    }

    companion object {
        const val VIDEOS_TOPIC = "/videos"
    }
}
