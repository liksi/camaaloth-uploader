package org.breizhcamp.video.uploader.ui.layouts

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.spring.annotation.SpringComponent
import com.vaadin.flow.spring.annotation.UIScope
import org.breizhcamp.video.uploader.services.EventSrv
import org.breizhcamp.video.uploader.services.FileSrv
import org.breizhcamp.video.uploader.services.VideoSrv
import org.breizhcamp.video.uploader.ui.components.SuccessNotification
import java.nio.file.Files

@SpringComponent
@UIScope
class LocalLayout(
    private val fileSrv: FileSrv,
    private val eventSrv: EventSrv,
    private val videoSrv: VideoSrv,
) : VerticalLayout() {

    private val logger = mu.KotlinLogging.logger {}
    private val dirExists = Files.isDirectory(fileSrv.recordingDir)

    init {
        verticalLayout {
            h3("Local")
            p("Répertoire des vidéos :")
            button {
                text = if (dirExists) "Recréer les répertoires des sessions" else "Créer le répertoire"
                onLeftClick {
                    logger.info { "Creating dir" }
                    fileSrv.createDirs()
                    SuccessNotification("Répertoire créé").open()
                }
            }
            button("Corriger les ids manquants dans schedule.json") {
                isVisible = dirExists
                onLeftClick {
                    logger.info { "Fix missing ids in schedule" }
                    eventSrv.generateMissingIdsAndWrite()
                    SuccessNotification("Correction des ids manquants dans les schedule.json effectuée").open()
                }
            }
            button("Exporter schedule.json") {
                isVisible = dirExists

                onLeftClick {
                    logger.info { "Generate schedule" }
                    videoSrv.generateUpdatedSchedule()
                    SuccessNotification("schedule.json généré").open()
                }
            }
        }
    }

}