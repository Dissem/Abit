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

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v7.app.NotificationCompat

import ch.dissem.apps.abit.MainActivity
import ch.dissem.apps.abit.R

/**
 * Ongoing notification while proof of work is in progress.
 */
class ProofOfWorkNotification(ctx: Context) : AbstractNotification(ctx) {

    init {
        update(0)
    }

    override fun getNotificationId(): Int {
        return ONGOING_NOTIFICATION_ID
    }

    fun update(numberOfItems: Int): ProofOfWorkNotification {
        val builder = NotificationCompat.Builder(ctx)

        val showMessageIntent = Intent(ctx, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(ctx, 0, showMessageIntent,
            PendingIntent.FLAG_UPDATE_CURRENT)

        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setUsesChronometer(true)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_notification_proof_of_work)
            .setContentTitle(ctx.getString(R.string.proof_of_work_title))
            .setContentText(if (numberOfItems == 0)
                ctx.getString(R.string.proof_of_work_text_0)
            else
                ctx.getString(R.string.proof_of_work_text_n, numberOfItems))
            .setContentIntent(pendingIntent)

        notification = builder.build()
        return this
    }

    companion object {
        @JvmField val ONGOING_NOTIFICATION_ID = 3
    }
}
