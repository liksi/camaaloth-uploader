package org.breizhcamp.video.uploader.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.breizhcamp.video.uploader.dto.Event;
import org.breizhcamp.video.uploader.dto.VideoInfo;
import org.breizhcamp.video.uploader.dto.VideoMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.breizhcamp.video.uploader.dto.VideoInfo.Status.DONE;
import static org.breizhcamp.video.uploader.dto.VideoInfo.Status.NOT_STARTED;
@Service
public class VideoSrv {

	@Autowired
	private EventSrv eventSrv;

	@Autowired
	private FileSrv fileSrv;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private  VideoProxySrv videoProxySrv;

	/**
	 * List all videos found in video directory
	 * @return VideoInfo found and status
	 */
	public List<VideoInfo> list() throws IOException {
		return videoProxySrv.list();
	}

	public void generateUpdatedSchedule() throws IOException {
		videoProxySrv.generateUpdatedSchedule();
	}

	/**
	 * Read a directory and create the associate video object
	 * @param dir Directory to read
	 * @return VideoInfo object filled or null if directory does not contains video file
	 */
	public VideoInfo readDir(Path dir) {
		return videoProxySrv.readDir(dir);
	}

	/**
	 * Update video status in metadata
	 * @param video Video to update
	 */
	public void updateVideo(VideoInfo video) throws IOException {
		videoProxySrv.updateVideo(video);
	}

	private static final Logger logger = LoggerFactory.getLogger(VideoSrv.class);
}
