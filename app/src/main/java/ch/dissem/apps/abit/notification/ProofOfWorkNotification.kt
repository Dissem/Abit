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
import ch.dissem.apps.abit.service.ProofOfWorkService
import ch.dissem.apps.abit.util.PowStats
import java.util.*
import kotlin.concurrent.fixedRateTimer

/**
 * Ongoing notification while proof of work is in progress.
 */
class ProofOfWorkNotification(ctx: Context) : AbstractNotification(ctx) {

    private val builder = NotificationCompat.Builder(ctx)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setUsesChronometer(true)
        .setOngoing(true)
        .setSmallIcon(R.drawable.ic_notification_proof_of_work)
        .setContentTitle(ctx.getString(R.string.proof_of_work_title))
    private var startTime = 0L
    private var progress = 0
    private var progressMax = 0

    private var timer: Timer? = null

    init {
        update(0)
    }

    override val notificationId = ONGOING_NOTIFICATION_ID

    fun update(numberOfItems: Int): ProofOfWorkNotification {

        val showMessageIntent = Intent(ctx, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(ctx, 0, showMessageIntent,
            PendingIntent.FLAG_UPDATE_CURRENT)

        builder.setContentText(if (numberOfItems == 0)
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

    fun start(item: ProofOfWorkService.PowItem) {
        val expectedPowTimeInMilliseconds = PowStats.getExpectedPowTimeInMilliseconds(ctx, item.targetValue)
        val delta = (expectedPowTimeInMilliseconds / 3).toInt()
        startTime = System.currentTimeMillis()
        progress = 0
        progressMax = delta
        builder.setProgress(progressMax, progress, false)
        notification = builder.build()
        show()

        timer = fixedRateTimer(initialDelay = 2000, period = 2000){
            val elapsedTime = System.currentTimeMillis() - startTime
            progress = elapsedTime.toInt()
            progressMax = progress + delta
            builder.setProgress(progressMax, progress, false)
            notification = builder.build()
            show()
        }
    }

    fun finished(item: ProofOfWorkService.PowItem) {
        timer?.cancel()
        progress = 0
        progressMax = 0
        if (showing) {
            builder.setProgress(0, 0, false)
            notification = builder.build()
            show()
        }
    }
}
