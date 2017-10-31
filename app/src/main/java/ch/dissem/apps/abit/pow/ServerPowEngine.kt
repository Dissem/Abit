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

package ch.dissem.apps.abit.pow

import android.content.Context
import ch.dissem.apps.abit.service.Singleton
import ch.dissem.apps.abit.synchronization.SyncAdapter
import ch.dissem.apps.abit.util.Preferences
import ch.dissem.bitmessage.InternalContext
import ch.dissem.bitmessage.extensions.CryptoCustomMessage
import ch.dissem.bitmessage.extensions.pow.ProofOfWorkRequest
import ch.dissem.bitmessage.extensions.pow.ProofOfWorkRequest.Request.CALCULATE
import ch.dissem.bitmessage.ports.ProofOfWorkEngine
import ch.dissem.bitmessage.utils.Singleton.cryptography
import org.slf4j.LoggerFactory
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * @author Christian Basler
 */
class ServerPowEngine(private val ctx: Context) : ProofOfWorkEngine, InternalContext.ContextHolder {
    private lateinit var context: InternalContext

    private val pool: ExecutorService

    init {
        pool = Executors.newCachedThreadPool { r ->
            val thread = Executors.defaultThreadFactory().newThread(r)
            thread.priority = Thread.MIN_PRIORITY
            thread
        }
    }

    override fun calculateNonce(initialHash: ByteArray, target: ByteArray, callback: ProofOfWorkEngine.Callback) =
        pool.execute {
            val identity = Singleton.getIdentity(ctx) ?: throw RuntimeException("No Identity for calculating POW")

            val request = ProofOfWorkRequest(identity, initialHash,
                    CALCULATE, target)
            SyncAdapter.startPowSync(ctx)
            try {
                val cryptoMsg = CryptoCustomMessage(request)
                cryptoMsg.signAndEncrypt(
                        identity,
                        cryptography().createPublicKey(identity.publicDecryptionKey)
                )
                val node = Preferences.getTrustedNode(ctx)
                if (node == null) {
                    LOG.error("trusted node is not defined")
                } else {
                    context.networkHandler.send(
                            node,
                            Preferences.getTrustedNodePort(ctx),
                            cryptoMsg)
                }
            } catch (e: Exception) {
                LOG.error(e.message, e)
            }
        }

    override fun setContext(context: InternalContext) {
        this.context = context
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ServerPowEngine::class.java)
    }
}
