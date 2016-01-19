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

package ch.dissem.apps.abit.synchronization;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import ch.dissem.apps.abit.service.Singleton;
import ch.dissem.apps.abit.util.Preferences;
import ch.dissem.bitmessage.BitmessageContext;
import ch.dissem.bitmessage.entity.BitmessageAddress;
import ch.dissem.bitmessage.entity.CustomMessage;
import ch.dissem.bitmessage.extensions.CryptoCustomMessage;
import ch.dissem.bitmessage.extensions.pow.ProofOfWorkRequest;
import ch.dissem.bitmessage.ports.ProofOfWorkRepository;

import static ch.dissem.apps.abit.synchronization.Authenticator.ACCOUNT_POW;
import static ch.dissem.apps.abit.synchronization.Authenticator.ACCOUNT_SYNC;
import static ch.dissem.apps.abit.synchronization.StubProvider.AUTHORITY;
import static ch.dissem.bitmessage.extensions.pow.ProofOfWorkRequest.Request.CALCULATE;
import static ch.dissem.bitmessage.extensions.pow.ProofOfWorkRequest.Request.COMPLETE;
import static ch.dissem.bitmessage.utils.Singleton.security;

/**
 * Sync Adapter to synchronize with the Bitmessage network - fetches
 * new objects and then disconnects.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private final static Logger LOG = LoggerFactory.getLogger(SyncAdapter.class);

    private static final long SYNC_FREQUENCY = 15 * 60; // seconds

    private final BitmessageContext bmc;

    /**
     * Set up the sync adapter
     */
    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        bmc = Singleton.getBitmessageContext(context);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {
        if (account.equals(Authenticator.ACCOUNT_SYNC)) {
            if (Preferences.isConnectionAllowed(getContext())) {
                syncData();
            }
        } else if (account.equals(Authenticator.ACCOUNT_POW)) {
            syncPOW();
        } else {
            throw new RuntimeException("Unknown " + account);
        }
    }

    private void syncData() {
        // If the Bitmessage context acts as a full node, synchronization isn't necessary
        if (bmc.isRunning()) {
            LOG.info("Synchronization skipped, Abit is acting as a full node");
            return;
        }
        LOG.info("Synchronizing Bitmessage");

        try {
            LOG.info("Synchronization started");
            bmc.synchronize(
                    Preferences.getTrustedNode(getContext()),
                    Preferences.getTrustedNodePort(getContext()),
                    Preferences.getTimeoutInSeconds(getContext()),
                    true);
            LOG.info("Synchronization finished");
        } catch (RuntimeException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void syncPOW() {
        // If the Bitmessage context acts as a full node, synchronization isn't necessary
        LOG.info("Looking for completed POW");

        try {
            BitmessageAddress identity = Singleton.getIdentity(getContext());
            byte[] privateKey = identity.getPrivateKey().getPrivateEncryptionKey();
            byte[] signingKey = security().createPublicKey(identity.getPublicDecryptionKey());
            ProofOfWorkRequest.Reader reader = new ProofOfWorkRequest.Reader(identity);
            ProofOfWorkRepository powRepo = Singleton.getProofOfWorkRepository(getContext());
            List<byte[]> items = powRepo.getItems();
            for (byte[] initialHash : items) {
                ProofOfWorkRepository.Item item = powRepo.getItem(initialHash);
                byte[] target = security().getProofOfWorkTarget(item.object, item
                        .nonceTrialsPerByte, item.extraBytes);
                CryptoCustomMessage<ProofOfWorkRequest> cryptoMsg = new CryptoCustomMessage<>(
                        new ProofOfWorkRequest(identity, initialHash, CALCULATE, target));
                cryptoMsg.signAndEncrypt(identity, signingKey);
                CustomMessage response = bmc.send(
                        Preferences.getTrustedNode(getContext()),
                        Preferences.getTrustedNodePort(getContext()),
                        cryptoMsg
                );
                if (response.isError()) {
                    LOG.error("Server responded with error: " + new String(response.getData(),
                            "UTF-8"));
                } else {
                    ProofOfWorkRequest decryptedResponse = CryptoCustomMessage.read(
                            response, reader).decrypt(privateKey);
                    if (decryptedResponse.getRequest() == COMPLETE) {
                        bmc.internals().getProofOfWorkService().onNonceCalculated(
                                initialHash, decryptedResponse.getData());
                    }
                }
            }
            if (items.size() == 0) {
                stopPowSync(getContext());
            }
            LOG.info("Synchronization finished");
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public static void startSync(Context ctx) {
        // Create account, if it's missing. (Either first run, or user has deleted account.)
        Account account = addAccount(ctx, ACCOUNT_SYNC);

        // Recommend a schedule for automatic synchronization. The system may modify this based
        // on other scheduled syncs and network utilization.
        ContentResolver.addPeriodicSync(account, AUTHORITY, new Bundle(), SYNC_FREQUENCY);
    }

    public static void stopSync(Context ctx) {
        // Create account, if it's missing. (Either first run, or user has deleted account.)
        Account account = addAccount(ctx, ACCOUNT_SYNC);

        ContentResolver.removePeriodicSync(account, AUTHORITY, new Bundle());
    }


    public static void startPowSync(Context ctx) {
        // Create account, if it's missing. (Either first run, or user has deleted account.)
        Account account = addAccount(ctx, ACCOUNT_POW);

        // Recommend a schedule for automatic synchronization. The system may modify this based
        // on other scheduled syncs and network utilization.
        ContentResolver.addPeriodicSync(account, AUTHORITY, new Bundle(), SYNC_FREQUENCY);
    }

    public static void stopPowSync(Context ctx) {
        // Create account, if it's missing. (Either first run, or user has deleted account.)
        Account account = addAccount(ctx, ACCOUNT_POW);

        ContentResolver.removePeriodicSync(account, AUTHORITY, new Bundle());
    }

    private static Account addAccount(Context ctx, Account account) {
        if (AccountManager.get(ctx).addAccountExplicitly(account, null, null)) {
            // Inform the system that this account supports sync
            ContentResolver.setIsSyncable(account, AUTHORITY, 1);
            // Inform the system that this account is eligible for auto sync when the network is up
            ContentResolver.setSyncAutomatically(account, AUTHORITY, true);
        }
        return account;
    }
}
