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

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import ch.dissem.apps.abit.MainActivity
import ch.dissem.apps.abit.R
import ch.dissem.apps.abit.service.BitmessageIntentService
import ch.dissem.apps.abit.service.NodeStartupService
import java.util.*
import kotlin.concurrent.fixedRateTimer

/**
 * Shows the network status (as long as the client is connected as a full node)
 */
class NetworkNotification(ctx: Context) : AbstractNotification(ctx) {

    private val builder = NotificationCompat.Builder(ctx, ONGOING_CHANNEL_ID)
    private var timer: Timer? = null

    init {
        initChannel(ONGOING_CHANNEL_ID, R.color.colorAccent)
        val showAppIntent = Intent(ctx, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(ctx, 1, showAppIntent, 0)
        builder
            .setSmallIcon(R.drawable.ic_notification_full_node_connecting)
            .setContentTitle(ctx.getString(R.string.bitmessage_full_node))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(false)
            .setContentIntent(pendingIntent)
    }

    @SuppressLint("StringFormatMatches")
    private fun update(): Boolean {
        val running = NodeStartupService.isRunning
        builder.setOngoing(running)
        val connections = NodeStartupService.status.getProperty("network", "connections")
        if (!running) {
            builder.setSmallIcon(R.drawable.ic_notification_full_node_disconnected)
            builder.setContentText(ctx.getString(R.string.connection_info_disconnected))
        } else if (connections == null || connections.properties.isEmpty()) {
            builder.setSmallIcon(R.drawable.ic_notification_full_node_connecting)
            builder.setContentText(ctx.getString(R.string.connection_info_pending))
        } else {
            builder.setSmallIcon(R.drawable.ic_notification_full_node)
            val info = StringBuilder()
            for (stream in connections.properties) {
                val streamNumber = Integer.parseInt(stream.name.substring("stream ".length))
                val nodeCount = stream.getProperty("nodes")!!.value as Int?
                if (nodeCount == 1) {
                    info.append(
                        ctx.getString(
                            R.string.connection_info_1,
                            streamNumber
                        )
                    )
                } else {
                    info.append(
                        ctx.getString(
                            R.string.connection_info_n,
                            streamNumber, nodeCount
                        )
                    )
                }
                info.append('\n')
            }
            builder.setContentText(info)
        }
        builder.mActions.clear()
        val intent = Intent(ctx, BitmessageIntentService::class.java)
        if (running) {
            intent.putExtra(BitmessageIntentService.EXTRA_SHUTDOWN_NODE, true)
            builder.addAction(
                R.drawable.ic_notification_node_stop,
                ctx.getString(R.string.full_node_stop),
                PendingIntent.getService(ctx, 0, intent, FLAG_UPDATE_CURRENT)
            )
        } else {
            intent.putExtra(BitmessageIntentService.EXTRA_STARTUP_NODE, true)
            builder.addAction(
                R.drawable.ic_notification_node_start,
                ctx.getString(R.string.full_node_restart),
                PendingIntent.getService(ctx, 1, intent, FLAG_UPDATE_CURRENT)
            )
        }
        notification = builder.build()
        return running
    }

    override fun show() {
        super.show()

        timer = fixedRateTimer(initialDelay = 10000, period = 10000) {
            if (!update()) {
                cancel()
            }
            super@NetworkNotification.show()
        }
    }

    fun showShutdown() {
        timer?.cancel()
        update()
        super.show()
    }

    override val notificationId = NETWORK_NOTIFICATION_ID

    fun connecting() {
        builder.setOngoing(true)
        builder.setContentText(ctx.getString(R.string.connection_info_pending))
        val intent = Intent(ctx, BitmessageIntentService::class.java)
        intent.putExtra(BitmessageIntentService.EXTRA_SHUTDOWN_NODE, true)
        builder.mActions.clear()
        builder.addAction(
            R.drawable.ic_notification_node_stop,
            ctx.getString(R.string.full_node_stop),
            PendingIntent.getService(ctx, 0, intent, FLAG_UPDATE_CURRENT)
        )
        notification = builder.build()
    }

    companion object {
        const val NETWORK_NOTIFICATION_ID = 2
    }
}
