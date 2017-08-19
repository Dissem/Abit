package ch.dissem.apps.abit.service

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Intent
import android.os.Build
import android.support.annotation.RequiresApi
import ch.dissem.apps.abit.util.Preferences

/**
 * Created by chrigu on 18.08.17.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class StartupNodeOnWifiService : JobService() {
    override fun onStartJob(params: JobParameters?): Boolean {
        val bmc = Singleton.getBitmessageContext(this)
        if (Preferences.isFullNodeActive(this) && !bmc.isRunning()) {
            applicationContext.startService(Intent(this, BitmessageService::class.java))
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        if (Preferences.isWifiOnly(this)) {
            Singleton.getBitmessageContext(this).shutdown()
            return Preferences.isFullNodeActive(this)
        } else {
            return false
        }
    }
}
