package ch.dissem.apps.abit.service

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import ch.dissem.apps.abit.notification.NetworkNotification
import ch.dissem.apps.abit.util.network
import ch.dissem.apps.abit.util.preferences
import ch.dissem.bitmessage.BitmessageContext
import ch.dissem.bitmessage.utils.Property
import org.jetbrains.anko.doAsync

/**
 * Starts the full node if
 * * it is active
 * * it is not already running
 *
 * And stops it when the preconditions for the job (unmetered network) aren't met anymore.
 */
class NodeStartupService : JobService() {
    private val bmc: BitmessageContext by lazy { Singleton.getBitmessageContext(this) }

    private lateinit var notification: NetworkNotification

    private val connectivityReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (bmc.isRunning() && !preferences.connectionAllowed) {
                bmc.shutdown()
            }
        }
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        if (preferences.online) {
            registerReceiver(
                connectivityReceiver,
                IntentFilter().apply {
                    addAction(ConnectivityManager.CONNECTIVITY_ACTION)
                    addAction(Intent.ACTION_BATTERY_CHANGED)
                }
            )
            notification = NetworkNotification(this)
            NodeStartupService.running = false

            if (!isRunning) {
                running = true
                notification.connecting()
                if (!bmc.isRunning()) {
                    bmc.startup()
                }
                notification.show()
            }
        }
        return true
    }

    override fun onDestroy() {
        if (bmc.isRunning()) {
            bmc.shutdown()
        }
        running = false
        notification.showShutdown()
        doAsync {
            bmc.cleanup()
        }
        unregisterReceiver(connectivityReceiver)
        stopSelf()
    }

    /**
     * Don't actually stop the service, otherwise it will be stopped after 1 or 10 minutes
     * depending on Android version.
     */
    override fun onStopJob(params: JobParameters?): Boolean {
        network.scheduleNodeStart()
        return false
    }

    companion object {
        @Volatile
        private var running = false

        val isRunning: Boolean
            get() = running && Singleton.bitmessageContext?.isRunning() == true

        val status: Property
            get() = Singleton.bitmessageContext?.status() ?: Property("bitmessage context")
    }
}
