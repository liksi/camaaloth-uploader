package org.breizhcamp.video.uploader.services

import org.apache.commons.lang3.StringUtils
import org.breizhcamp.video.uploader.dto.Event
import java.time.format.DateTimeFormatter

object PathUtils {

    private val dayFormat = DateTimeFormatter.ofPattern("dd")
    private val timeFormat = DateTimeFormatter.ofPattern("HH-mm")

    /**
     * Retrieve event id from it's path name
     * @param path Path to retrieve id from
     * @return Id of the event, null if not found
     */
    fun getIdFromPath(path: String): String? {
        val dash = path.lastIndexOf('-')
        return if (dash < 0) {
            null
        } else path.substring(dash + 2)

    }

    fun buildDirName(talk: Event): String {
        val name = cleanForFilename(talk.name)
        val speakers = cleanForFilename(talk.speakers)

        return (dayFormat.format(talk.eventStart) + "." + talk.venue + "." + timeFormat.format(talk.eventStart)
                + " - " + name + " (" + speakers + ") - " + talk.id)
    }

    private fun cleanForFilename(str: String?) = str?.let { str ->
        StringUtils.stripAccents(str)
            .replace(Regex("[\\\\/:*?\"<>|]"), "-")
            .replace(Regex("[^A-Za-z,\\-\\\\ ]"), "")
    }
}