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
class VideoSrv(private val fileSrv: FileSrv, private val objectMapper: ObjectMapper,  private val eventSrv: EventSrv) {

    fun list(): List<VideoInfo> = if (!Files.isDirectory(fileSrv.recordingDir)) emptyList() else
        Files.list(fileSrv.recordingDir).asSequence()
            .filter { Files.isDirectory(it) }
            .mapNotNull { readDir(it) }
            .sortedBy { it.dirName }
            .toList()

    fun generateUpdatedSchedule() {
        val completedUploadsUrls = list()
            .filter { it.status == VideoInfo.Status.DONE }
            .associate { it.eventId to it.youtubeId }

        val updatedEvents = eventSrv.read()
            .onEach { event ->
                completedUploadsUrls[event.id]?.let {
                    event.videoUrl = "https://www.youtube.com/watch?v=$it"
                }
            }
            .sortedBy { it.id }

        objectMapper.writeValue(fileSrv.recordingDir.resolve("schedule.json").toFile(), updatedEvents)
    }

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
        videoInfo.eventId = PathUtils.getIdFromPath(dir.fileName.toString())

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

    fun updateVideo(video: VideoInfo) {
        val statusFile = requireNotNull(video.path) { "Cannot update metadata of [${video.dirName}] because no path defined" }

        val metadata = VideoMetadata(
            status = video.status,
            progression = video.progression,
            youtubeId = video.youtubeId
        )

        statusFile.parent.resolve("metadata.json").let {
            objectMapper.writeValue(it.toFile(), metadata)
        }
    }

    /**
     * List a directory to retrieve the first file with the specified extension
     * @param dir Directory to read
     * @param ext Extension to find
     * @return First file found or null if any file with specified extension exists within the directory
     */
    private fun getFirstFileFromExt(dir: Path, vararg ext: String): Path? {
        if (!Files.isDirectory(dir)) return null

        val suffixes = ext.map { ".$it" }
        return Files.list(dir).asSequence()
            .firstOrNull { f -> suffixes.any { f.toString().lowercase().endsWith(it) } }
    }
}