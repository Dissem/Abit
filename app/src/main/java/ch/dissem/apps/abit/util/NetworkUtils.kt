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


object NetworkUtils {

    fun enableNode(ctx: Context, ask: Boolean = true) {
        if (Preferences.isConnectionAllowed(ctx) || !ask) {
            scheduleNodeStart(ctx)
        } else {
            // Ask for connection
            val dialogIntent = Intent(ctx, FullNodeDialogActivity::class.java)
            if (ctx !is Activity) {
                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
            }
            ctx.startActivity(dialogIntent)
        }
    }

    fun doStartBitmessageService(ctx: Context) {
        ctx.startService(Intent(ctx, BitmessageService::class.java))
    }

    fun disableNode(ctx: Context) {
        ctx.stopService(Intent(ctx, BitmessageService::class.java))
    }

    fun scheduleNodeStart(ctx: Context) {
        val jobScheduler = ctx.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val serviceComponent = ComponentName(ctx, NodeStartupService::class.java)
        val builder = JobInfo.Builder(0, serviceComponent)
        when {
            Preferences.isWifiOnly(ctx) ->
                builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ->
                builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_NOT_ROAMING)
            else ->
                builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        }
        if (Preferences.requireCharging(ctx)) {
            builder.setRequiresCharging(true)
        }
        builder.setPeriodic(15 * 60 * 1000L)
        builder.setPersisted(true)
        jobScheduler.schedule(builder.build())
    }
}
