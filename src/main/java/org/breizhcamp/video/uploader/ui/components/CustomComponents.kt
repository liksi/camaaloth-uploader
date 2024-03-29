package org.breizhcamp.video.uploader.ui.components

import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.notification.NotificationVariant

class SuccessNotification(text: String) :  Notification(text) {
    init {
        duration = 1000
        addThemeVariants(NotificationVariant.LUMO_SUCCESS)
    }
}

class ErrorNotification(throwable: Throwable) :  Notification(throwable.localizedMessage) {
    init {
        duration = 2000
        addThemeVariants(NotificationVariant.LUMO_ERROR)
    }
}