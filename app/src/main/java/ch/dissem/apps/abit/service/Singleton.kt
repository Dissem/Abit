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

import android.content.Context
import android.widget.Toast
import ch.dissem.apps.abit.MainActivity
import ch.dissem.apps.abit.R
import ch.dissem.apps.abit.adapter.AndroidCryptography
import ch.dissem.apps.abit.adapter.SwitchingProofOfWorkEngine
import ch.dissem.apps.abit.listener.MessageListener
import ch.dissem.apps.abit.pow.ServerPowEngine
import ch.dissem.apps.abit.repository.*
import ch.dissem.apps.abit.util.Constants
import ch.dissem.bitmessage.BitmessageContext
import ch.dissem.bitmessage.entity.BitmessageAddress
import ch.dissem.bitmessage.entity.payload.Pubkey
import ch.dissem.bitmessage.networking.nio.NioNetworkHandler
import ch.dissem.bitmessage.ports.DefaultLabeler
import ch.dissem.bitmessage.utils.ConversationService
import ch.dissem.bitmessage.utils.TTL
import ch.dissem.bitmessage.utils.UnixTime.DAY
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

/**
 * Provides singleton objects across the application.
 */
object Singleton {
    val labeler = DefaultLabeler()
    var bitmessageContext: BitmessageContext? = null
        private set
    private var conversationService: ConversationService? = null
    private var messageListener: MessageListener? = null
    private var identity: BitmessageAddress? = null
    private var powRepo: AndroidProofOfWorkRepository? = null
    private var creatingIdentity: Boolean = false

    fun getBitmessageContext(context: Context): BitmessageContext {
        return init({ bitmessageContext }, { bitmessageContext = it }) {
            val ctx = context.applicationContext
            val sqlHelper = SqlHelper(ctx)
            val powRepo = AndroidProofOfWorkRepository(sqlHelper)
            this.powRepo = powRepo
            TTL.pubkey = 2 * DAY
            BitmessageContext.Builder()
                    .proofOfWorkEngine(SwitchingProofOfWorkEngine(
                            ctx, Constants.PREFERENCE_SERVER_POW,
                            ServerPowEngine(ctx),
                            ServicePowEngine(ctx)
                    ))
                    .cryptography(AndroidCryptography())
                    .nodeRegistry(AndroidNodeRegistry(sqlHelper))
                    .inventory(AndroidInventory(sqlHelper))
                    .addressRepo(AndroidAddressRepository(sqlHelper))
                    .messageRepo(AndroidMessageRepository(sqlHelper, ctx))
                    .powRepo(powRepo)
                    .networkHandler(NioNetworkHandler())
                    .listener(getMessageListener(ctx))
                    .labeler(labeler)
                    .doNotSendPubkeyOnIdentityCreation()
                    .build()
        }
    }

    fun getMessageListener(ctx: Context) = init({ messageListener }, { messageListener = it }) { MessageListener(ctx) }

    fun getMessageRepository(ctx: Context) = getBitmessageContext(ctx).messages as AndroidMessageRepository

    fun getAddressRepository(ctx: Context) = getBitmessageContext(ctx).addresses as AndroidAddressRepository

    fun getProofOfWorkRepository(ctx: Context) = powRepo ?: getBitmessageContext(ctx).internals.proofOfWorkRepository

    fun getIdentity(ctx: Context): BitmessageAddress? {
        return init<BitmessageAddress?>(ctx, { identity }, { identity = it }) { bmc ->
            val identities = bmc.addresses.getIdentities()
            if (identities.isNotEmpty()) {
                identities[0]
            } else {
                if (!creatingIdentity) {
                    creatingIdentity = true
                    doAsync {
                        val identity = bmc.createIdentity(false,
                                Pubkey.Feature.DOES_ACK)
                        identity.alias = ctx.getString(R.string.alias_default_identity)
                        bmc.addresses.save(identity)

                        uiThread {
                            Singleton.identity = identity
                            Toast.makeText(ctx,
                                    R.string.toast_identity_created,
                                    Toast.LENGTH_SHORT).show()
                            val mainActivity = MainActivity.getInstance()
                            mainActivity?.addIdentityEntry(identity)
                        }
                    }
                }
                null
            }
        }
    }

    fun setIdentity(identity: BitmessageAddress) {
        if (identity.privateKey == null)
            throw IllegalArgumentException("Identity expected, but no private key available")
        Singleton.identity = identity
    }

    fun getConversationService(ctx: Context) = init(ctx, { conversationService }, { conversationService = it }) { ConversationService(it.messages) }

    private inline fun <T> init(crossinline getter: () -> T?, crossinline setter: (T) -> Unit, crossinline creator: () -> T): T {
        return getter() ?: {
            synchronized(Singleton) {
                getter() ?: {
                    val v = creator()
                    setter(v)
                    v
                }.invoke()
            }
        }.invoke()
    }

    private inline fun <T> init(ctx: Context, crossinline getter: () -> T?, crossinline setter: (T) -> Unit, crossinline creator: (BitmessageContext) -> T): T {
        return getter() ?: {
            val bmc = getBitmessageContext(ctx)
            synchronized(Singleton) {
                getter() ?: {
                    val v = creator(bmc)
                    setter(v)
                    v
                }.invoke()
            }
        }.invoke()
    }
}
