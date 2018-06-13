package ch.dissem.apps.abit.util

import android.app.Activity
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.support.v4.content.ContextCompat
import ch.dissem.apps.abit.dialog.FullNodeDialogActivity
import ch.dissem.apps.abit.service.BitmessageService
import ch.dissem.apps.abit.service.StartupNodeOnWifiService


object NetworkUtils {

    fun enableNode(ctx: Context, ask: Boolean = true) {
        Preferences.setFullNodeActive(ctx, true)

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
        ContextCompat.startForegroundService(ctx, Intent(ctx, BitmessageService::class.java))
    }

    fun disableNode(ctx: Context) {
        Preferences.setFullNodeActive(ctx, false)
        ctx.stopService(Intent(ctx, BitmessageService::class.java))
    }

    fun scheduleNodeStart(ctx: Context) {
        val jobScheduler = ctx.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val serviceComponent = ComponentName(ctx, StartupNodeOnWifiService::class.java)
        val builder = JobInfo.Builder(0, serviceComponent)
        if (Preferences.isWifiOnly(ctx)) {
            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
        }
        if (Preferences.requireCharging(ctx)) {
            builder.setRequiresCharging(true)
        }
        builder.setBackoffCriteria(0L, JobInfo.BACKOFF_POLICY_LINEAR)
        builder.setPersisted(true)
        jobScheduler.schedule(builder.build())
    }
}
