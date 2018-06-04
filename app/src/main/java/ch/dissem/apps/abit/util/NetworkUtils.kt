package ch.dissem.apps.abit.util

import android.app.Activity
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v4.content.ContextCompat
import ch.dissem.apps.abit.MainActivity
import ch.dissem.apps.abit.dialog.FullNodeDialogActivity
import ch.dissem.apps.abit.service.BitmessageService
import ch.dissem.apps.abit.service.StartupNodeOnWifiService


object NetworkUtils {

    fun enableNode(ctx: Context, ask: Boolean = true) {
        Preferences.setFullNodeActive(ctx, true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (Preferences.isConnectionAllowed(ctx) || !ask) {
                scheduleNodeStart(ctx)
            } else {
                askForConnection(ctx)
            }
        } else {
            if (Preferences.isWifiOnly(ctx)) {
                if (Preferences.isConnectionAllowed(ctx)) {
                    doStartBitmessageService(ctx)
                    MainActivity.updateNodeSwitch()
                } else if (ask) {
                    askForConnection(ctx)
                }
            } else {
                doStartBitmessageService(ctx)
                MainActivity.updateNodeSwitch()
            }
        }
    }

    private fun askForConnection(ctx: Context) {
        val dialogIntent = Intent(ctx, FullNodeDialogActivity::class.java)
        if (ctx !is Activity) {
            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
        }
        ctx.startActivity(dialogIntent)
    }

    fun doStartBitmessageService(ctx: Context) {
        ContextCompat.startForegroundService(ctx, Intent(ctx, BitmessageService::class.java))
    }

    fun disableNode(ctx: Context) {
        Preferences.setFullNodeActive(ctx, false)
        ctx.stopService(Intent(ctx, BitmessageService::class.java))
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun scheduleNodeStart(ctx: Context) {
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
        val jobScheduler = ctx.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        jobScheduler.schedule(builder.build())
    }
}
