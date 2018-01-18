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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder

import java.util.LinkedList

import ch.dissem.apps.abit.service.ProofOfWorkService.PowBinder
import ch.dissem.apps.abit.service.ProofOfWorkService.PowItem
import ch.dissem.bitmessage.ports.ProofOfWorkEngine

import android.content.Context.BIND_AUTO_CREATE

/**
 * Proof of Work engine that uses the Proof of Work service.
 */
class ServicePowEngine(private val ctx: Context) : ProofOfWorkEngine {
    private val queue = LinkedList<PowItem>()
    private var service: PowBinder? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) = synchronized(lock) {
            this@ServicePowEngine.service = service as PowBinder
            while (!queue.isEmpty()) {
                service.process(queue.poll())
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            service = null
        }
    }

    override fun calculateNonce(initialHash: ByteArray, target: ByteArray, callback: ProofOfWorkEngine.Callback) {
        val item = PowItem(initialHash, target, callback)
        synchronized(lock) {
            service?.process(item) ?: {
                queue.add(item)
                ctx.bindService(Intent(ctx, ProofOfWorkService::class.java), connection, BIND_AUTO_CREATE)
            }.invoke()
        }
    }

    companion object {
        private val lock = Any()
    }
}
