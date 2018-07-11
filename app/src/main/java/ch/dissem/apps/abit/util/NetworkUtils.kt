package ch.dissem.apps.abit.util

import android.app.Activity
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import ch.dissem.apps.abit.dialog.FullNodeDialogActivity
import ch.dissem.apps.abit.service.BitmessageService
import ch.dissem.apps.abit.service.NodeStartupService
import java.lang.ref.WeakReference

val Context.network get() = NetworkUtils.getInstance(this)

class NetworkUtils internal constructor(private val ctx: Context) {

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
        ctx.stopService(Intent(ctx, BitmessageService::class.java))
    }

    fun scheduleNodeStart() {
        val jobScheduler = ctx.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val serviceComponent = ComponentName(ctx, NodeStartupService::class.java)
        val builder = JobInfo.Builder(0, serviceComponent)
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
