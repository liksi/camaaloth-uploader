package org.breizhcamp.video.uploader.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import org.breizhcamp.video.uploader.dto.VideoInfo
import org.breizhcamp.video.uploader.dto.VideoMetadata
import org.springframework.stereotype.Service
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.asSequence

private val logger = KotlinLogging.logger {}

@Service
class VideoProxySrv(private val fileSrv: FileSrv, private val objectMapper: ObjectMapper) {

    fun list(): List<VideoInfo> = if (!Files.isDirectory(fileSrv.recordingDir)) emptyList() else
        Files.list(fileSrv.recordingDir).asSequence()
            .filter { Files.isDirectory(it) }
            .mapNotNull { readDir(it) }
            .sortedBy { it.dirName }
            .toList()

    fun readDir(dir: Path): VideoInfo? {
        //retrieving first video file
        val videoFile: Path = getFirstFileFromExt(dir, "mp4") ?: return null
        val thumbnail = dir.resolve("thumb.png")
        val videoInfo = VideoInfo()
        videoInfo.path = videoFile
        videoInfo.status = VideoInfo.Status.NOT_STARTED
        if (thumbnail.toFile().exists()) {
            videoInfo.thumbnail = thumbnail
        }
        videoInfo.eventId = fileSrv.getIdFromPath(dir.fileName.toString())

        val statusFile = dir.resolve("metadata.json")
        if (Files.exists(statusFile)) {
            try {
                val metadata = objectMapper.readValue<VideoMetadata>(statusFile.toFile())
                videoInfo.status = metadata.status
                videoInfo.progression = metadata.progression
                videoInfo.youtubeId = metadata.youtubeId
            } catch (e: IOException) {
                logger.error("Unable to read metadata in {}", statusFile)
                throw e
            }
        }
        return videoInfo
    }

    private fun getFirstFileFromExt(dir: Path, vararg ext: String): Path? {
        if (!Files.isDirectory(dir)) return null

        val suffixes = ext.map { ".$it" }
        return Files.list(dir).asSequence()
            .firstOrNull { f -> suffixes.any { f.toString().lowercase().endsWith(it) } }
    }
}