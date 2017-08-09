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

import android.content.Context
import android.support.annotation.StringRes
import android.support.v7.app.NotificationCompat

import ch.dissem.apps.abit.R

/**
 * Easily create notifications with error messages. Use carefully, users probably won't like them.
 * (But they are useful during development/testing)

 * @author Christian Basler
 */
class ErrorNotification(ctx: Context) : AbstractNotification(ctx) {

    private val builder = NotificationCompat.Builder(ctx)
        .setContentTitle(ctx.getString(R.string.app_name))
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

    fun setWarning(@StringRes resId: Int, vararg args: Any): ErrorNotification {
        builder.setSmallIcon(R.drawable.ic_notification_warning)
            .setContentText(ctx.getString(resId, *args))
        notification = builder.build()
        return this
    }

    fun setError(@StringRes resId: Int, vararg args: Any): ErrorNotification {
        builder.setSmallIcon(R.drawable.ic_notification_error)
            .setContentText(ctx.getString(resId, *args))
        notification = builder.build()
        return this
    }

    override val notificationId = ERROR_NOTIFICATION_ID

    companion object {
        val ERROR_NOTIFICATION_ID = 4
    }
}
