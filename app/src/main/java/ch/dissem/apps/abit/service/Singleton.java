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

package ch.dissem.apps.abit.service;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import java.util.List;

import ch.dissem.apps.abit.MainActivity;
import ch.dissem.apps.abit.R;
import ch.dissem.apps.abit.adapter.AndroidCryptography;
import ch.dissem.apps.abit.adapter.SwitchingProofOfWorkEngine;
import ch.dissem.apps.abit.listener.MessageListener;
import ch.dissem.apps.abit.pow.ServerPowEngine;
import ch.dissem.apps.abit.repository.AndroidAddressRepository;
import ch.dissem.apps.abit.repository.AndroidInventory;
import ch.dissem.apps.abit.repository.AndroidMessageRepository;
import ch.dissem.apps.abit.repository.AndroidNodeRegistry;
import ch.dissem.apps.abit.repository.AndroidProofOfWorkRepository;
import ch.dissem.apps.abit.repository.SqlHelper;
import ch.dissem.apps.abit.util.Constants;
import ch.dissem.bitmessage.BitmessageContext;
import ch.dissem.bitmessage.entity.BitmessageAddress;
import ch.dissem.bitmessage.entity.payload.Pubkey;
import ch.dissem.bitmessage.networking.nio.NioNetworkHandler;
import ch.dissem.bitmessage.ports.AddressRepository;
import ch.dissem.bitmessage.ports.MessageRepository;
import ch.dissem.bitmessage.ports.ProofOfWorkRepository;
import ch.dissem.bitmessage.utils.ConversationService;
import ch.dissem.bitmessage.utils.TTL;

import static ch.dissem.bitmessage.utils.UnixTime.DAY;

/**
 * Provides singleton objects across the application.
 */
public class Singleton {
    private static BitmessageContext bitmessageContext;
    private static ConversationService conversationService;
    private static MessageListener messageListener;
    private static BitmessageAddress identity;
    private static AndroidProofOfWorkRepository powRepo;
    private static boolean creatingIdentity;

    public static BitmessageContext getBitmessageContext(Context context) {
        if (bitmessageContext == null) {
            synchronized (Singleton.class) {
                if (bitmessageContext == null) {
                    final Context ctx = context.getApplicationContext();
                    SqlHelper sqlHelper = new SqlHelper(ctx);
                    powRepo = new AndroidProofOfWorkRepository(sqlHelper);
                    TTL.pubkey(2 * DAY);
                    bitmessageContext = new BitmessageContext.Builder()
                        .proofOfWorkEngine(new SwitchingProofOfWorkEngine(
                            ctx, Constants.PREFERENCE_SERVER_POW,
                            new ServerPowEngine(ctx),
                            new ServicePowEngine(ctx)
                        ))
                        .cryptography(new AndroidCryptography())
                        .nodeRegistry(new AndroidNodeRegistry(sqlHelper))
                        .inventory(new AndroidInventory(sqlHelper))
                        .addressRepo(new AndroidAddressRepository(sqlHelper))
                        .messageRepo(new AndroidMessageRepository(sqlHelper, ctx))
                        .powRepo(powRepo)
                        .networkHandler(new NioNetworkHandler())
                        .listener(getMessageListener(ctx))
                        .doNotSendPubkeyOnIdentityCreation()
                        .build();
                }
            }
        }
        return bitmessageContext;
    }

    public static MessageListener getMessageListener(Context ctx) {
        if (messageListener == null) {
            synchronized (Singleton.class) {
                if (messageListener == null) {
                    messageListener = new MessageListener(ctx);
                }
            }
        }
        return messageListener;
    }

    public static MessageRepository getMessageRepository(Context ctx) {
        return getBitmessageContext(ctx).messages();
    }

    public static AddressRepository getAddressRepository(Context ctx) {
        return getBitmessageContext(ctx).addresses();
    }

    public static ProofOfWorkRepository getProofOfWorkRepository(Context ctx) {
        if (powRepo == null) getBitmessageContext(ctx);
        return powRepo;
    }

    public static BitmessageAddress getIdentity(final Context ctx) {
        if (identity == null) {
            final BitmessageContext bmc = getBitmessageContext(ctx);
            synchronized (Singleton.class) {
                if (identity == null) {
                    List<BitmessageAddress> identities = bmc.addresses()
                        .getIdentities();
                    if (identities.size() > 0) {
                        identity = identities.get(0);
                    } else {
                        if (!creatingIdentity) {
                            creatingIdentity = true;
                            new AsyncTask<Void, Void, BitmessageAddress>() {
                                @Override
                                protected BitmessageAddress doInBackground(Void... args) {
                                    BitmessageAddress identity = bmc.createIdentity(false,
                                        Pubkey.Feature.DOES_ACK);
                                    identity.setAlias(
                                        ctx.getString(R.string.alias_default_identity)
                                    );
                                    bmc.addresses().save(identity);
                                    return identity;
                                }

                                @Override
                                protected void onPostExecute(BitmessageAddress identity) {
                                    Singleton.identity = identity;
                                    Toast.makeText(ctx,
                                        R.string.toast_identity_created,
                                        Toast.LENGTH_SHORT).show();
                                    MainActivity mainActivity = MainActivity.getInstance();
                                    if (mainActivity != null) {
                                        mainActivity.addIdentityEntry(identity);
                                    }
                                }
                            }.execute();
                        }
                        return null;
                    }
                }
            }
        }
        return identity;
    }

    public static void setIdentity(BitmessageAddress identity) {
        if (identity.getPrivateKey() == null)
            throw new IllegalArgumentException("Identity expected, but no private key available");
        Singleton.identity = identity;
    }

    public static ConversationService getConversationService(Context ctx) {
        if (conversationService == null) {
            final BitmessageContext bmc = getBitmessageContext(ctx);
            synchronized (Singleton.class) {
                if (conversationService == null) {
                    conversationService = new ConversationService(bmc.messages());
                }
            }
        }
        return conversationService;
    }
}
