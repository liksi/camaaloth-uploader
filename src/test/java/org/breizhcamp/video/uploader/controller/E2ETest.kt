package org.breizhcamp.video.uploader.controller

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.api.services.youtube.model.*
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import okhttp3.mockwebserver.MockWebServer
import org.apache.commons.io.FileUtils
import org.breizhcamp.video.uploader.CamaalothUploaderProps
import org.breizhcamp.video.uploader.dto.Event
import org.breizhcamp.video.uploader.enqueueObject
import org.breizhcamp.video.uploader.verifyRequest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpMethod
import org.springframework.test.context.ActiveProfiles
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.regex.Pattern

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("e2e")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class E2ETest {

    @LocalServerPort
    lateinit var port: String

    @Autowired
    lateinit var props: CamaalothUploaderProps;

    val mapper = jacksonMapperBuilder().addModule(JavaTimeModule()).build()

    private val ytServer= MockWebServer()

    @BeforeAll
    fun setUp() {
        ytServer.start(20000)
    }

    @AfterAll
    fun tearDown() {
        ytServer.shutdown()
    }

    @Test
    fun `should authenticate youtube and load channels and playlists`() {

        val channels = mutableListOf(Channel().apply {
            id = UUID.randomUUID().toString()
            snippet = ChannelSnippet().apply {
                country = "France"
                description = "Toutes les vidéos du BreizhCamp, LA conférence informatique à Rennes qu'il ne faut pas rater."
                title = "BreizhCamp"
                thumbnails = ThumbnailDetails().apply {
                    default = Thumbnail().apply {
                        url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
                    }
                }
            }
        })

        val playlistItems = mutableListOf(Playlist().apply {
            snippet = PlaylistSnippet().apply {
                title = "test playlist snippet"
                id = "1"
                description = "test playlist description"
            }
        })

        ytServer.enqueueObject(ChannelListResponse().apply { items = channels })
        ytServer.enqueueObject(PlaylistListResponse().apply { items = playlistItems })

        Playwright.create().use {
            val page = launchAppliOnFirefox(it)

            val authBtn = page.locator("#yt-auth")
            authBtn.click()

            ytServer.verifyRequest(
                requestedPath = "/youtube/v3/channels?mine=true&part=id,snippet",
                requestedMethod = HttpMethod.GET,
            )

            assertThat(page.locator("#yt-auth")).isVisible()
        }
    }

    @Test
    fun `should create sessions directory`(){

        var videosDir = Paths.get(props.recordingDir).toAbsolutePath()
        var scheduleFile: File = Paths.get(props.assetsDir, "schedule.json").toFile()

        FileUtils.deleteDirectory(videosDir.toFile())
        Playwright.create().use {
            val page = launchAppliOnFirefox(it)

            val createDirBtn = page.locator("#createDir")
            createDirBtn.click()

            assertThat(page.locator("#createDir")).not().isVisible()
            assertThat(page.locator("#reCreateDir")).isVisible()
            assert(checkAllFolderHaveBeenCreated(videosDir, scheduleFile))
        }
    }

    @Test
    fun `should fix ids on schedule`(){

        var scheduleFile: File = Paths.get(props.assetsDir, "schedule.json").toFile()
        var savedScheduleFile: File = Paths.get(props.assetsDir, "schedule_save.json").toFile()

        saveAndInitScheduleFileForTest(scheduleFile, savedScheduleFile)

        Playwright.create().use {
            val page = launchAppliOnFirefox(it)

            val fixMissingIdsBtn = page.locator("#fixMissingIds")
            fixMissingIdsBtn.click()

            assertThat(page.locator("#fixMissingIds")).isVisible()
            assert(mapper.readValue<List<Event>>(scheduleFile).filter { it.name == "Test" }.first().id != null)
            assert(mapper.readValue<List<Event>>(scheduleFile).all {it.id != null})

            resetScheduleFile(savedScheduleFile, scheduleFile)
        }
    }


    private fun launchAppliOnFirefox(it: Playwright): Page {
        val browser = it.firefox().launch(BrowserType.LaunchOptions().setHeadless(false).setSlowMo(1000.0))
        val page = browser.newPage()
        page.navigate("http://localhost:$port")
        return page
    }

    private fun checkAllFolderHaveBeenCreated(videosDir: Path, scheduleFile: File): Boolean {
        val events = mapper.readValue<List<Event>>(scheduleFile)
        return Files.exists(videosDir.toAbsolutePath()) && events.all { currentEvent ->
            return videosDir.toFile().listFiles()?.any{
                Pattern.matches(".+" + currentEvent.name + ".+", Pattern.quote(it.absolutePath)) } ?: false
        }
    }

    private fun saveAndInitScheduleFileForTest(scheduleFile: File, savedScheduleFile: File) {
        FileUtils.copyFile(scheduleFile, savedScheduleFile);
        val events = mapper.readValue<List<Event>>(scheduleFile).toMutableList()
        events.add(Event().apply { name = "Test"  })
        mapper.writeValue(scheduleFile, events)
    }

    private fun resetScheduleFile(savedScheduleFile: File, scheduleFile: File) {
        FileUtils.copyFile(savedScheduleFile, scheduleFile);
        FileUtils.forceDelete(savedScheduleFile);
    }
}
