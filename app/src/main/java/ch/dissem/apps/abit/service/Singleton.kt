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
import android.os.AsyncTask
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
import ch.dissem.bitmessage.ports.ProofOfWorkRepository
import ch.dissem.bitmessage.utils.ConversationService
import ch.dissem.bitmessage.utils.TTL
import ch.dissem.bitmessage.utils.UnixTime.DAY

/**
 * Provides singleton objects across the application.
 */
object Singleton {
    var bitmessageContext: BitmessageContext? = null
        private set
    private var conversationService: ConversationService? = null
    private var messageListener: MessageListener? = null
    private var identity: BitmessageAddress? = null
    private var powRepo: AndroidProofOfWorkRepository? = null
    private var creatingIdentity: Boolean = false

    @JvmStatic
    fun getBitmessageContext(context: Context): BitmessageContext {
        if (bitmessageContext == null) {
            synchronized(Singleton::class.java) {
                if (bitmessageContext == null) {
                    val ctx = context.applicationContext
                    val sqlHelper = SqlHelper(ctx)
                    powRepo = AndroidProofOfWorkRepository(sqlHelper)
                    TTL.pubkey = 2 * DAY
                    bitmessageContext = BitmessageContext.Builder()
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
                        .powRepo(powRepo!!)
                        .networkHandler(NioNetworkHandler())
                        .listener(getMessageListener(ctx))
                        .doNotSendPubkeyOnIdentityCreation()
                        .build()
                }
            }
        }
        return bitmessageContext!!
    }

    @JvmStatic
    fun getMessageListener(ctx: Context): MessageListener {
        if (messageListener == null) {
            synchronized(Singleton::class.java) {
                if (messageListener == null) {
                    messageListener = MessageListener(ctx)
                }
            }
        }
        return messageListener!!
    }

    @JvmStatic
    fun getMessageRepository(ctx: Context): AndroidMessageRepository {
        return getBitmessageContext(ctx).messages as AndroidMessageRepository
    }

    @JvmStatic
    fun getAddressRepository(ctx: Context): AndroidAddressRepository {
        return getBitmessageContext(ctx).addresses as AndroidAddressRepository
    }

    @JvmStatic
    fun getProofOfWorkRepository(ctx: Context): ProofOfWorkRepository {
        if (powRepo == null) getBitmessageContext(ctx)
        return powRepo!!
    }

    @JvmStatic
    fun getIdentity(ctx: Context): BitmessageAddress? {
        if (identity == null) {
            val bmc = getBitmessageContext(ctx)
            synchronized(Singleton::class) {
                if (identity == null) {
                    val identities = bmc.addresses.getIdentities()
                    if (identities.isNotEmpty()) {
                        identity = identities[0]
                    } else {
                        if (!creatingIdentity) {
                            creatingIdentity = true
                            object : AsyncTask<Void, Void, BitmessageAddress>() {
                                override fun doInBackground(vararg args: Void): BitmessageAddress {
                                    val identity = bmc.createIdentity(false,
                                        Pubkey.Feature.DOES_ACK)
                                    identity.alias = ctx.getString(R.string.alias_default_identity)
                                    bmc.addresses.save(identity)
                                    return identity
                                }

                                override fun onPostExecute(identity: BitmessageAddress) {
                                    Singleton.identity = identity
                                    Toast.makeText(ctx,
                                        R.string.toast_identity_created,
                                        Toast.LENGTH_SHORT).show()
                                    val mainActivity = MainActivity.getInstance()
                                    mainActivity?.addIdentityEntry(identity)
                                }
                            }.execute()
                        }
                        return null
                    }
                }
            }
        }
        return identity
    }

    @JvmStatic
    fun setIdentity(identity: BitmessageAddress) {
        if (identity.privateKey == null)
            throw IllegalArgumentException("Identity expected, but no private key available")
        Singleton.identity = identity
    }

    @JvmStatic
    fun getConversationService(ctx: Context): ConversationService {
        if (conversationService == null) {
            val bmc = getBitmessageContext(ctx)
            synchronized(Singleton::class.java) {
                if (conversationService == null) {
                    conversationService = ConversationService(bmc.messages)
                }
            }
        }
        return conversationService!!
    }
}
