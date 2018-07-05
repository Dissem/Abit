package ch.dissem.apps.abit.service

import android.app.job.JobParameters
import android.app.job.JobService
import ch.dissem.apps.abit.util.NetworkUtils
import ch.dissem.apps.abit.util.Preferences

/**
 * Starts the full node if
 * * it is active
 * * it is not already running
 *
 * And stops it when the preconditions for the job (unmetered network) aren't met anymore.
 */
class NodeStartupService : JobService() {

    override fun onStartJob(params: JobParameters?): Boolean {
        val bmc = Singleton.getBitmessageContext(this)
        if (Preferences.isOnline(this) && !bmc.isRunning()) {
            NetworkUtils.doStartBitmessageService(applicationContext)
        }
        return true
    }

    /**
     * Don't actually stop the service, otherwise it will be stopped after 1 or 10 minutes
     * depending on Android version.
     */
    override fun onStopJob(params: JobParameters?) = false

}
