package org.breizhcamp.video.uploader.ui

import com.github.mvysny.karibudsl.v10.KComposite
import com.github.mvysny.karibudsl.v10.h1
import com.github.mvysny.karibudsl.v10.horizontalLayout
import com.github.mvysny.karibudsl.v10.verticalLayout
import com.vaadin.flow.router.Route
import org.breizhcamp.video.uploader.ui.layouts.LocalLayout
import org.breizhcamp.video.uploader.ui.layouts.VideosList
import org.breizhcamp.video.uploader.ui.layouts.YoutubeLayout

@Route("")
class UploaderView(
    private val localLayout: LocalLayout,
    private val youtubeLayout: YoutubeLayout,
    private val videosList: VideosList
) : KComposite() {

    private val root = ui {
        verticalLayout {
            setWidthFull()
            h1("BreizhCamp Video Uploader")
            horizontalLayout {
                add(localLayout, youtubeLayout)
            }
            add(videosList)
        }
    }
}