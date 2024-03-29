package org.breizhcamp.video.uploader.ui.layouts

import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.kaributools.setPrimary
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.spring.annotation.SpringComponent
import com.vaadin.flow.spring.annotation.UIScope
import org.breizhcamp.video.uploader.dto.YoutubeSession
import org.breizhcamp.video.uploader.services.YoutubeSrv
import org.breizhcamp.video.uploader.ui.components.ErrorNotification
import org.springframework.core.style.StylerUtils.style


@SpringComponent
@UIScope
class YoutubeLayout(
    private val youtubeSrv: YoutubeSrv,
    private val ytSession: YoutubeSession,
) : VerticalLayout() {
    init {
        verticalLayout {
            horizontalLayout {
                h3("Youtube")
                button {
                    isVisible = youtubeSrv.isConnected()
                    icon = Icon(VaadinIcon.REFRESH)
                    setPrimary()
                    onLeftClick {
                        runCatching {
                            reloadYtSession()
                        }.onFailure {
                            ErrorNotification(it).open()
                        }
                    }
                }

                button("S'authentifier sur YouTube") {
                    isVisible = !youtubeSrv.isConnected()
                    icon = Icon(VaadinIcon.SIGN_IN)
                    setPrimary()
                    onLeftClick {
                        runCatching {
                            auth()
                        }.onFailure {
                            ErrorNotification(it).open()
                        }
                    }
                }
                horizontalLayout {
                    isVisible = youtubeSrv.isConnected()
                    image {
                        src = ytSession.curChan?.snippet?.thumbnails?.default?.url.orEmpty()
                        style {
                            width = "50px"
                            height = "50px"
                        }
                    }
                    h4(ytSession.curChan?.snippet?.title.orEmpty())
                    /**
                     * TODO
                     * <form method="post" th:action="@{/yt/curPlaylist}" class="form-inline" id="form-playlist">
                     * 							<div class="form-group">
                     * 								<label for="select-playlist">Playlist</label>
                     * 								<select id="select-playlist" name="playlist" class="form-control">
                     * 									<option value="none">Aucune</option>
                     * 									<option th:each="p : ${ytSession.playlistsSorted()}"
                     * 											th:value="${p.id}" th:text="${p.snippet.title}"
                     * 											th:selected="${ytSession.curPlaylist != null && p.id == ytSession.curPlaylist.id}"></option>
                     * 								</select>
                     * 							</div>
                     * 						</form>
                     */
                    button("Changer de compte") {
                        icon = Icon(VaadinIcon.SIGN_OUT)
                        onLeftClick {
                            runCatching {
                                auth()
                            }.onFailure {
                                ErrorNotification(it).open()
                            }
                        }
                    }
                }
            }
        }
    }

    fun auth() {
        // FIXME
        youtubeSrv.handleAuth("code", "redirectUrl")
        reloadYtSession()
    }

    fun reloadYtSession() {
        val channels = youtubeSrv.getChannels()
        ytSession.channels = channels
        if (channels.size == 1) {
            ytSession.apply {
                curChan = ytSession.channels!![0]
                playlists = youtubeSrv.getPlaylists(ytSession.curChan!!.id)
            }
        }
    }
}