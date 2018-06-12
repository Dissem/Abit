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
import android.support.v4.app.NotificationCompat
import ch.dissem.apps.abit.R
import ch.dissem.apps.abit.service.Job

/**
 * Ongoing notification while proof of work is in progress.
 */
class BatchNotification(ctx: Context) : AbstractNotification(ctx) {

    private val builder = NotificationCompat.Builder(ctx, ONGOING_CHANNEL_ID)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setUsesChronometer(true)

    init {
        initChannel(ONGOING_CHANNEL_ID, R.color.colorAccent)
        notification = builder.build()
    }

    override val notificationId = ONGOING_NOTIFICATION_ID

    fun update(job: Job): BatchNotification {

        builder.setContentTitle(ctx.getString(job.description))
            .setSmallIcon(job.icon)
            .setProgress(job.numberOfItems, job.numberOfProcessedItems, job.numberOfItems <= 0)

        notification = builder.build()
        show()
        return this
    }

    companion object {
        const val ONGOING_NOTIFICATION_ID = 4
    }
}
