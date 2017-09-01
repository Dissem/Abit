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

package ch.dissem.apps.abit.synchronization

import android.accounts.Account
import android.accounts.AccountManager
import android.content.*
import android.os.Bundle
import ch.dissem.apps.abit.service.Singleton
import ch.dissem.apps.abit.synchronization.Authenticator.Companion.ACCOUNT_POW
import ch.dissem.apps.abit.synchronization.Authenticator.Companion.ACCOUNT_SYNC
import ch.dissem.apps.abit.synchronization.StubProvider.Companion.AUTHORITY
import ch.dissem.apps.abit.util.Preferences
import ch.dissem.bitmessage.exception.DecryptionFailedException
import ch.dissem.bitmessage.extensions.CryptoCustomMessage
import ch.dissem.bitmessage.extensions.pow.ProofOfWorkRequest
import ch.dissem.bitmessage.extensions.pow.ProofOfWorkRequest.Request.CALCULATE
import ch.dissem.bitmessage.extensions.pow.ProofOfWorkRequest.Request.COMPLETE
import ch.dissem.bitmessage.utils.Singleton.cryptography
import org.slf4j.LoggerFactory
import java.io.IOException

/**
 * Sync Adapter to synchronize with the Bitmessage network - fetches
 * new objects and then disconnects.
 */
class SyncAdapter(context: Context, autoInitialize: Boolean) : AbstractThreadedSyncAdapter(context, autoInitialize) {

    private val bmc = Singleton.getBitmessageContext(context)

    override fun onPerformSync(
            account: Account,
            extras: Bundle,
            authority: String,
            provider: ContentProviderClient,
            syncResult: SyncResult
    ) {
        try {
            if (account == ACCOUNT_SYNC) {
                if (Preferences.isConnectionAllowed(context)) {
                    syncData()
                }
            } else if (account == ACCOUNT_POW) {
                syncPOW()
            } else {
                syncResult.stats.numAuthExceptions++
            }
        } catch (e: IOException) {
            syncResult.stats.numIoExceptions++
        } catch (e: DecryptionFailedException) {
            syncResult.stats.numAuthExceptions++
        }

    }

    private fun syncData() {
        // If the Bitmessage context acts as a full node, synchronization isn't necessary
        if (bmc.isRunning()) {
            LOG.info("Synchronization skipped, Abit is acting as a full node")
            return
        }
        val trustedNode = Preferences.getTrustedNode(context)
        if (trustedNode == null) {
            LOG.info("Trusted node not available, disabling synchronization")
            stopSync(context)
            return
        }
        LOG.info("Synchronization started")
        bmc.synchronize(
                trustedNode,
                Preferences.getTrustedNodePort(context),
                Preferences.getTimeoutInSeconds(context),
                true
        )
        LOG.info("Synchronization finished")
    }

    private fun syncPOW() {
        val identity = Singleton.getIdentity(context)
        if (identity == null) {
            LOG.info("No identity available - skipping POW synchronization")
            return
        }
        val trustedNode = Preferences.getTrustedNode(context)
        if (trustedNode == null) {
            LOG.info("Trusted node not available, disabling POW synchronization")
            stopPowSync(context)
            return
        }
        // If the Bitmessage context acts as a full node, synchronization isn't necessary
        LOG.info("Looking for completed POW")

        val privateKey = identity.privateKey?.privateEncryptionKey ?: throw IllegalStateException("Identity without private key")
        val signingKey = cryptography().createPublicKey(identity.publicDecryptionKey)
        val reader = ProofOfWorkRequest.Reader(identity)
        val powRepo = Singleton.getProofOfWorkRepository(context)
        val items = powRepo.getItems()
        for (initialHash in items) {
            val (objectMessage, nonceTrialsPerByte, extraBytes) = powRepo.getItem(initialHash)
            val target = cryptography().getProofOfWorkTarget(objectMessage, nonceTrialsPerByte, extraBytes)
            val cryptoMsg = CryptoCustomMessage(
                    ProofOfWorkRequest(identity, initialHash, CALCULATE, target))
            cryptoMsg.signAndEncrypt(identity, signingKey)
            val response = bmc.send(
                    trustedNode,
                    Preferences.getTrustedNodePort(context),
                    cryptoMsg
            )
            if (response.isError) {
                LOG.error("Server responded with error: ${String(response.getData())}")
            } else {
                val (_, _, request, data) = CryptoCustomMessage.read(response, reader).decrypt(privateKey)
                if (request == COMPLETE) {
                    bmc.internals.proofOfWorkService.onNonceCalculated(initialHash, data)
                }
            }
        }
        if (items.isEmpty()) {
            stopPowSync(context)
        }
        LOG.info("Synchronization finished")
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(SyncAdapter::class.java)

        private const val SYNC_FREQUENCY = 15 * 60L // seconds

        fun startSync(ctx: Context) {
            // Create account, if it's missing. (Either first run, or user has deleted account.)
            val account = addAccount(ctx, ACCOUNT_SYNC)

            // Recommend a schedule for automatic synchronization. The system may modify this based
            // on other scheduled syncs and network utilization.
            ContentResolver.addPeriodicSync(account, AUTHORITY, Bundle(), SYNC_FREQUENCY)
        }

        fun stopSync(ctx: Context) {
            // Create account, if it's missing. (Either first run, or user has deleted account.)
            val account = addAccount(ctx, ACCOUNT_SYNC)

            ContentResolver.removePeriodicSync(account, AUTHORITY, Bundle())
        }

        fun startPowSync(ctx: Context) {
            // Create account, if it's missing. (Either first run, or user has deleted account.)
            val account = addAccount(ctx, ACCOUNT_POW)

            // Recommend a schedule for automatic synchronization. The system may modify this based
            // on other scheduled syncs and network utilization.
            ContentResolver.addPeriodicSync(account, AUTHORITY, Bundle(), SYNC_FREQUENCY)
        }

        fun stopPowSync(ctx: Context) {
            // Create account, if it's missing. (Either first run, or user has deleted account.)
            val account = addAccount(ctx, ACCOUNT_POW)

            ContentResolver.removePeriodicSync(account, AUTHORITY, Bundle())
        }

        private fun addAccount(ctx: Context, account: Account): Account {
            if (AccountManager.get(ctx).addAccountExplicitly(account, null, null)) {
                // Inform the system that this account supports sync
                ContentResolver.setIsSyncable(account, AUTHORITY, 1)
                // Inform the system that this account is eligible for auto sync when the network is up
                ContentResolver.setSyncAutomatically(account, AUTHORITY, true)
            }
            return account
        }
    }
}
