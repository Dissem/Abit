package ch.dissem.apps.abit.service

import android.app.job.JobParameters
import android.app.job.JobService
import org.jetbrains.anko.doAsync

class CleanupService : JobService() {

    override fun onStartJob(params: JobParameters?): Boolean {
        doAsync {
            Singleton.getBitmessageContext(this@CleanupService).cleanup()
            jobFinished(params, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters?) = false
}
