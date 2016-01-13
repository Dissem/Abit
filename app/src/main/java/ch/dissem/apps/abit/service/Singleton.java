package ch.dissem.apps.abit.service;

import android.content.Context;

import java.util.List;

import ch.dissem.apps.abit.adapter.AndroidCryptography;
import ch.dissem.apps.abit.adapter.SwitchingProofOfWorkEngine;
import ch.dissem.apps.abit.listener.MessageListener;
import ch.dissem.apps.abit.pow.ServerPowEngine;
import ch.dissem.apps.abit.repository.AndroidAddressRepository;
import ch.dissem.apps.abit.repository.AndroidInventory;
import ch.dissem.apps.abit.repository.AndroidMessageRepository;
import ch.dissem.apps.abit.repository.AndroidProofOfWorkRepository;
import ch.dissem.apps.abit.repository.SqlHelper;
import ch.dissem.apps.abit.util.Constants;
import ch.dissem.bitmessage.BitmessageContext;
import ch.dissem.bitmessage.entity.BitmessageAddress;
import ch.dissem.bitmessage.networking.DefaultNetworkHandler;
import ch.dissem.bitmessage.ports.AddressRepository;
import ch.dissem.bitmessage.ports.MemoryNodeRegistry;
import ch.dissem.bitmessage.ports.MessageRepository;
import ch.dissem.bitmessage.ports.ProofOfWorkRepository;

import static ch.dissem.bitmessage.utils.UnixTime.DAY;

/**
 * Provides singleton objects across the application.
 */
public class Singleton {
    public static final Object lock = new Object();
    private static BitmessageContext bitmessageContext;
    private static MessageListener messageListener;
    private static BitmessageAddress identity;
    private static AndroidProofOfWorkRepository powRepo;

    public static BitmessageContext getBitmessageContext(Context context) {
        if (bitmessageContext == null) {
            synchronized (lock) {
                if (bitmessageContext == null) {
                    final Context ctx = context.getApplicationContext();
                    SqlHelper sqlHelper = new SqlHelper(ctx);
                    powRepo = new AndroidProofOfWorkRepository(sqlHelper);
                    bitmessageContext = new BitmessageContext.Builder()
                            .proofOfWorkEngine(new SwitchingProofOfWorkEngine(
                                    ctx, Constants.PREFERENCE_SERVER_POW,
                                    new ServerPowEngine(ctx),
                                    new ServicePowEngine(ctx)
                            ))
                            .cryptography(new AndroidCryptography())
                            .nodeRegistry(new MemoryNodeRegistry())
                            .inventory(new AndroidInventory(sqlHelper))
                            .addressRepo(new AndroidAddressRepository(sqlHelper))
                            .messageRepo(new AndroidMessageRepository(sqlHelper, ctx))
                            .powRepo(powRepo)
                            .networkHandler(new DefaultNetworkHandler())
                            .listener(getMessageListener(ctx))
                            .doNotSendPubkeyOnIdentityCreation()
                            .pubkeyTTL(2 * DAY)
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

    public static BitmessageAddress getIdentity(Context ctx) {
        if (identity == null) {
            synchronized (Singleton.class) {
                if (identity == null) {
                    List<BitmessageAddress> identities = getBitmessageContext(ctx).addresses()
                            .getIdentities();
                    if (identities.size() > 0) {
                        identity = identities.get(0);
                    }
                }
            }
        }
        return identity;
    }
}
