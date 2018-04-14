package ch.dissem.apps.abit.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.support.v4.content.ContextCompat
import ch.dissem.apps.abit.notification.BatchNotification
import ch.dissem.apps.abit.notification.BatchNotification.Companion.ONGOING_NOTIFICATION_ID
import org.jetbrains.anko.doAsync
import java.util.*

class BatchProcessorService : Service() {
    private lateinit var notification: BatchNotification

    override fun onCreate() {
        notification = BatchNotification(this)
    }

    override fun onBind(intent: Intent) = BatchBinder(this)

    class BatchBinder internal constructor(val service: BatchProcessorService) : Binder() {
        private val notification = service.notification

        fun process(job: Job) = synchronized(queue) {
            ContextCompat.startForegroundService(
                service,
                Intent(service, BatchProcessorService::class.java)
            )
            service.startForeground(
                ONGOING_NOTIFICATION_ID,
                notification.notification
            )
            if (!working) {
                working = true
                service.processQueue(job)
            } else {
                queue.add(job)
            }
        }

    }

    private fun processQueue(job: Job) {
        doAsync {
            var next: Job? = job
            while (next != null) {
                next.process(notification)

                synchronized(queue) {
                    next = queue.poll()
                    if (next == null) {
                        working = false
                        stopForeground(true)
                        stopSelf()
                    }
                }
            }

        }
    }

    companion object {
        private var working = false
        private val queue = LinkedList<Job>()
    }
}

interface Job {
    val icon: Int
        @DrawableRes get

    val description: Int
        @StringRes get

    val numberOfItems: Int
    var numberOfProcessedItems: Int

    /**
     * Runs the job. This shouldn't happen in a separate thread, as this is handled by the service.
     */
    fun process(notification: BatchNotification)
}

data class SimpleJob<T>(
    override val numberOfItems: Int,
    /**
     * Provides the next batch of items, given the last item of the previous batch,
     * or null for the first batch.
     */
    private val provider: (T?) -> List<T>,
    /**
     * Processes an item.
     */
    private val processor: (T) -> Unit,
    override val icon: Int,
    override val description: Int
) : Job {
    override var numberOfProcessedItems: Int = 0

    override fun process(notification: BatchNotification) {
        notification.update(this)
        var batch = provider.invoke(null)
        while (batch.isNotEmpty()) {
            Thread.yield()
            batch.forEach {
                processor.invoke(it)
                Thread.yield()
            }
            numberOfProcessedItems += batch.size
            notification.update(this)
            batch = provider.invoke(batch.last())
        }
    }

}
