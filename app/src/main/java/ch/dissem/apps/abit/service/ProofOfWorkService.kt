/*
 * Copyright 2016 Christian Basler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.dissem.apps.abit.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.support.v4.content.ContextCompat
import ch.dissem.apps.abit.notification.ProofOfWorkNotification
import ch.dissem.apps.abit.notification.ProofOfWorkNotification.Companion.ONGOING_NOTIFICATION_ID
import ch.dissem.apps.abit.util.PowStats
import ch.dissem.bitmessage.ports.MultiThreadedPOWEngine
import ch.dissem.bitmessage.ports.ProofOfWorkEngine
import java.util.*

/**
 * The Proof of Work Service makes sure POW is done in a foreground process, so it shouldn't be
 * killed by the system before the nonce is found.
 */
class ProofOfWorkService : Service() {
    private lateinit var notification: ProofOfWorkNotification

    override fun onCreate() {
        notification = ProofOfWorkNotification(this)
    }

    override fun onBind(intent: Intent) = PowBinder(this)

    class PowBinder internal constructor(private val service: ProofOfWorkService) : Binder() {
        private val notification = service.notification

        fun process(item: PowItem) = synchronized(queue) {
            ContextCompat.startForegroundService(
                service,
                Intent(service, ProofOfWorkService::class.java)
            )
            service.startForeground(
                ONGOING_NOTIFICATION_ID,
                notification.notification
            )
            if (!calculating) {
                calculating = true
                service.calculateNonce(item)
            } else {
                queue.add(item)
                notification.update(queue.size).show()
            }
        }
    }


    data class PowItem(
        val initialHash: ByteArray,
        val targetValue: ByteArray,
        val callback: ProofOfWorkEngine.Callback
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as PowItem

            if (!Arrays.equals(initialHash, other.initialHash)) return false
            if (!Arrays.equals(targetValue, other.targetValue)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = Arrays.hashCode(initialHash)
            result = 31 * result + Arrays.hashCode(targetValue)
            return result
        }
    }

    private fun calculateNonce(item: PowItem) {
        notification.start(item)
        val startTime = System.currentTimeMillis()
        engine.calculateNonce(
            item.initialHash,
            item.targetValue,
            object : ProofOfWorkEngine.Callback {
                override fun onNonceCalculated(initialHash: ByteArray, nonce: ByteArray) {
                    notification.finished()
                    val time = System.currentTimeMillis() - startTime
                    PowStats.addPow(this@ProofOfWorkService, time, item.targetValue)
                    try {
                        item.callback.onNonceCalculated(initialHash, nonce)
                    } finally {
                        var next: PowItem? = null
                        synchronized(queue) {
                            next = queue.poll()
                            if (next == null) {
                                calculating = false
                                stopForeground(true)
                                stopSelf()
                            } else {
                                notification.update(queue.size).show()
                            }
                        }
                        next?.let { calculateNonce(it) }
                    }
                }
            })
    }

    companion object {
        // Object to use as a thread-safe lock
        private val engine = MultiThreadedPOWEngine()
        private val queue = LinkedList<PowItem>()
        private var calculating: Boolean = false
    }
}
