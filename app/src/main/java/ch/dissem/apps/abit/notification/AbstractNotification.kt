/*
 * Copyright 2016 Christian Basler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.dissem.apps.abit.notification

import android.app.Notification
import android.app.NotificationManager
import android.content.Context

/**
 * Some base class to create and handle notifications.
 */
abstract class AbstractNotification(ctx: Context) {
    protected val ctx = ctx.applicationContext
    protected val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    var notification: Notification? = null
        protected set
    protected var showing = false
        private set

    /**
     * @return an id unique to this notification class
     */
    protected abstract val notificationId: Int

    open fun show() {
        manager.notify(notificationId, notification)
        showing = true
    }

    fun hide() {
        showing = false
        manager.cancel(notificationId)
    }
}
