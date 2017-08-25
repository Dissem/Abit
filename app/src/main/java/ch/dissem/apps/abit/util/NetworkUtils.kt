package ch.dissem.apps.abit.util

import android.app.Activity
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.annotation.RequiresApi
import ch.dissem.apps.abit.MainActivity
import ch.dissem.apps.abit.dialog.FullNodeDialogActivity
import ch.dissem.apps.abit.service.BitmessageService
import ch.dissem.apps.abit.service.StartupNodeOnWifiService


object NetworkUtils {

    fun enableNode(ctx: Context, ask: Boolean = true) {
        Preferences.setFullNodeActive(ctx, true)
        if (Preferences.isWifiOnly(ctx)) {
            if (Preferences.isConnectionAllowed(ctx)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    scheduleNodeStart(ctx)
                } else {
                    ctx.startService(Intent(ctx, BitmessageService::class.java))
                    MainActivity.updateNodeSwitch()
                }
            } else if (ask) {
                val dialogIntent = Intent(ctx, FullNodeDialogActivity::class.java)
                if (ctx !is Activity) {
                    dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ctx.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
                }
                ctx.startActivity(dialogIntent)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                scheduleNodeStart(ctx)
            }
        } else {
            ctx.startService(Intent(ctx, BitmessageService::class.java))
            MainActivity.updateNodeSwitch()
        }
    }

    fun disableNode(ctx: Context) {
        Preferences.setFullNodeActive(ctx, false)
        ctx.stopService(Intent(ctx, BitmessageService::class.java))
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun scheduleNodeStart(ctx: Context) {
        val serviceComponent = ComponentName(ctx, StartupNodeOnWifiService::class.java)
        val builder = JobInfo.Builder(0, serviceComponent)
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
        builder.setBackoffCriteria(0L, JobInfo.BACKOFF_POLICY_LINEAR)
        val jobScheduler = ctx.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        jobScheduler.schedule(builder.build())
    }
}
