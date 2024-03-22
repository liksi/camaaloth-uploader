package org.breizhcamp.video.uploader.view

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.router.Route
import org.breizhcamp.video.uploader.dto.YoutubeSession
import org.breizhcamp.video.uploader.services.EventSrv
import org.breizhcamp.video.uploader.services.FileSrv
import org.breizhcamp.video.uploader.services.VideoSrv
import org.breizhcamp.video.uploader.services.YoutubeSrv
import org.springframework.core.style.StylerUtils.style
import java.nio.file.Files

@Route("")
class UploaderView(
    private val youtubeSrv: YoutubeSrv,
    private val ytSession: YoutubeSession,
    private val fileSrv: FileSrv,
    private val eventSrv: EventSrv,
    private val videoSrv: VideoSrv,

    ) : KComposite() {

    private val logger = mu.KotlinLogging.logger {}
    private val dirExists = Files.isDirectory(fileSrv.recordingDir)
    private val connected = youtubeSrv.isConnected()

    private val root = ui {
        verticalLayout {
            h1("BreizhCamp Video Uploader")
            horizontalLayout {
                createLocalLayout()
                createYoutubeLayout()
            }
        }
    }

    private fun @VaadinDsl HorizontalLayout.createYoutubeLayout() {
        verticalLayout {
            div {

                h3("Youtube") {
                    anchor {
                        // TODO il est trop petit le composant :D
                        isVisible = connected
                        href = "/yt/reload"
                        setTitle("Recharger les infos de l'utilisateur connecté")
                        span {
                            className = "glyphicon glyphicon-refresh"
                            style {
                                width = "5px"
                                height = "5px"
                            }
                        }
                    }
                }
                anchor("S'authentifier sur YouTube") {
                    isVisible = !connected
                    className = "btn btn-primary"
                    href = "/yt/auth"
                }
                div {
                    isVisible = connected
                    div("media") {
                        div("media-left") {
                            image {
                                src = ytSession.curChan?.snippet?.thumbnails?.default?.url.orEmpty()
                                style {
                                    width = "50px"
                                    height = "50px"
                                }
                            }
                        }
                        div("media-body") {
                            h4(ytSession.curChan?.snippet?.title.orEmpty()) {
                                className = "media-heading"
                            }
                        }
                        // TODO https://github.com/liksi/camaaloth-uploader/blob/96c75945dbead3ab267b399f72e37b72848d7106/src/main/resources/templates/index.html#L58
                    }
                }
            }
        }
    }

    private fun @VaadinDsl HorizontalLayout.createLocalLayout() {
        verticalLayout {
            h3("Local")
            p("Répertoire des vidéos :")
            button {
                text = if (dirExists) "Recréer les répertoires des sessions" else "Créer le répertoire"
                onLeftClick {
                    logger.info { "Creating dir" }
                    fileSrv.createDirs()
                }
            }
            button("Corriger les ids manquants dans schedule.json") {
                isVisible = dirExists
                onLeftClick {
                    logger.info { "Fix missing ids in schedule" }
                    eventSrv.generateMissingIdsAndWrite()
                }
            }
            button("Exporter schedule.json") {
                isVisible = dirExists

                onLeftClick {
                    logger.info { "Generate schedule" }
                    videoSrv.generateUpdatedSchedule()
                }
            }
        }
    }
}