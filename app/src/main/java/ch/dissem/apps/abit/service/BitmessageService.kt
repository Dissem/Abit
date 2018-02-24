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

package ch.dissem.apps.abit.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Handler
import ch.dissem.apps.abit.notification.NetworkNotification
import ch.dissem.apps.abit.notification.NetworkNotification.Companion.NETWORK_NOTIFICATION_ID
import ch.dissem.apps.abit.util.Preferences
import ch.dissem.bitmessage.BitmessageContext
import ch.dissem.bitmessage.utils.Property

/**
 * Define a Service that returns an IBinder for the
 * sync adapter class, allowing the sync adapter framework to call
 * onPerformSync().
 */
class BitmessageService : Service() {

    private val bmc: BitmessageContext by lazy { Singleton.getBitmessageContext(this) }
    private lateinit var notification: NetworkNotification

    private val connectivityReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (bmc.isRunning() && !Preferences.isConnectionAllowed(this@BitmessageService)) {
                bmc.shutdown()
            }
        }
    }

    private val cleanupHandler = Handler()
    private val cleanupTask: Runnable = object : Runnable {
        override fun run() {
            bmc.cleanup()
            if (isRunning) {
                cleanupHandler.postDelayed(this, 24 * 60 * 60 * 1000L) // once a day
            }
        }
    }

    override fun onCreate() {
        registerReceiver(
            connectivityReceiver,
            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        )
        notification = NetworkNotification(this)
        running = false
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            running = true
            notification.connecting()
            startForeground(NETWORK_NOTIFICATION_ID, notification.notification)
            if (!bmc.isRunning()) {
                bmc.startup()
            }
            notification.show()
            cleanupHandler.postDelayed(cleanupTask, 24 * 60 * 60 * 1000L)
        }
        return Service.START_STICKY
    }

    override fun onDestroy() {
        if (bmc.isRunning()) {
            bmc.shutdown()
        }
        running = false
        notification.showShutdown()
        cleanupHandler.removeCallbacks(cleanupTask)
        bmc.cleanup()
        unregisterReceiver(connectivityReceiver)
        stopSelf()
    }

    override fun onBind(intent: Intent) = null

    companion object {
        @Volatile
        private var running = false

        val isRunning: Boolean
            get() = running && Singleton.bitmessageContext?.isRunning() == true

        val status: Property
            get() = Singleton.bitmessageContext?.status() ?: Property("bitmessage context")
    }
}
