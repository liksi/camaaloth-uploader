package org.breizhcamp.video.uploader.dto

import com.fasterxml.jackson.annotation.JsonInclude

import java.math.BigDecimal

/**
 * JSON file stored aside of the video file to keep record of the current status
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class VideoMetadata(
    var status: VideoInfo.Status? = null,
    var progression: BigDecimal? = null,
    var youtubeId: String? = null,
)