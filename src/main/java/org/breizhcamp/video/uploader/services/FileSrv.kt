package org.breizhcamp.video.uploader.services

import org.breizhcamp.video.uploader.CamaalothUploaderProps
import org.breizhcamp.video.uploader.dto.Event
import org.springframework.stereotype.Service
import org.apache.commons.lang3.StringUtils.stripAccents

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.format.DateTimeFormatter

/**
 * Handle directory and files operations
 */
@Service
class FileSrv(private val eventSrv: EventSrv, private val props: CamaalothUploaderProps) {

    val recordingDir: Path
        get() = Paths.get(props.recordingDir).toAbsolutePath()

    /**
     * Create one directory for each event in videosDir.
     */
    @Throws(IOException::class)
    fun createDirs() {
        Files.createDirectories(recordingDir)
        val events = eventSrv.read()

        for (event in events) {
            val dir = recordingDir.resolve(PathUtils.buildDirName(event))
            if (!Files.exists(dir)) {
                Files.createDirectory(dir)
            }
        }
    }

}
