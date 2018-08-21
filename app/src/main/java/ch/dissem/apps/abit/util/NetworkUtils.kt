package ch.dissem.apps.abit.util

import android.app.Activity
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import ch.dissem.apps.abit.dialog.FullNodeDialogActivity
import ch.dissem.apps.abit.service.CleanupService
import ch.dissem.apps.abit.service.NodeStartupService
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

val Context.network get() = NetworkUtils.getInstance(this)

class NetworkUtils internal constructor(private val ctx: Context) {

    private val jobScheduler by lazy { ctx.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler }

    fun enableNode(ask: Boolean = true) {
        if (ask && !ctx.preferences.connectionAllowed) {
            // Ask for connection
            val dialogIntent = Intent(ctx, FullNodeDialogActivity::class.java)
            if (ctx !is Activity) {
                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
            }
            ctx.startActivity(dialogIntent)
        } else {
            scheduleNodeStart()
        }
    }

    fun disableNode() {
        jobScheduler.cancelAll()
    }

    fun scheduleNodeStart() {
        JobInfo.Builder(0, ComponentName(ctx, NodeStartupService::class.java)).let { builder ->
            when {
                ctx.preferences.wifiOnly ->
                    builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ->
                    builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_NOT_ROAMING)
                else ->
                    builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            }
            builder.setRequiresCharging(ctx.preferences.requireCharging)
            builder.setPersisted(true)

            jobScheduler.schedule(builder.build())
        }

        JobInfo.Builder(1, ComponentName(ctx, CleanupService::class.java)).let { builder ->
            builder.setPeriodic(TimeUnit.DAYS.toMillis(1))
            builder.setRequiresDeviceIdle(true)
            builder.setRequiresCharging(true)

            jobScheduler.schedule(builder.build())
        }
    }

    companion object {
        private var instance: WeakReference<NetworkUtils>? = null

        internal fun getInstance(ctx: Context): NetworkUtils {
            var networkUtils = instance?.get()
            if (networkUtils == null) {
                networkUtils = NetworkUtils(ctx.applicationContext)
                instance = WeakReference(networkUtils)
            }
            return networkUtils
        }
    }
}
