package org.breizhcamp.video.uploader.ui.layouts

import com.github.mvysny.karibudsl.v10.button
import com.github.mvysny.karibudsl.v10.columnFor
import com.github.mvysny.karibudsl.v10.grid
import com.github.mvysny.karibudsl.v10.h3
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.spring.annotation.SpringComponent
import com.vaadin.flow.spring.annotation.UIScope
import org.breizhcamp.video.uploader.dto.VideoInfo
import org.breizhcamp.video.uploader.services.VideoSrv


@SpringComponent
@UIScope
class VideosList(
    private val videoSrv: VideoSrv
) : VerticalLayout() {
    init {
        h3("Vidéos")
        grid<VideoInfo> {
            setItems(videoSrv.list())
            columnFor(VideoInfo::dirName).setHeader("Nom répertoire")
            columnFor(VideoInfo::status).setHeader("État")
            addColumn {
                button("Uploader sur youtube") {
                    icon = VaadinIcon.EDIT.create()
                    addClickListener { }

                }
            }
        }


    }
}