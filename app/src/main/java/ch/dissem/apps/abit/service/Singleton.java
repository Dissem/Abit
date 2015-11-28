package ch.dissem.apps.abit.service;

import android.content.Context;

import ch.dissem.apps.abit.adapter.AndroidSecurity;
import ch.dissem.apps.abit.listener.MessageListener;
import ch.dissem.apps.abit.repository.AndroidAddressRepository;
import ch.dissem.apps.abit.repository.AndroidInventory;
import ch.dissem.apps.abit.repository.AndroidMessageRepository;
import ch.dissem.apps.abit.repository.SqlHelper;
import ch.dissem.bitmessage.BitmessageContext;
import ch.dissem.bitmessage.networking.DefaultNetworkHandler;
import ch.dissem.bitmessage.ports.AddressRepository;
import ch.dissem.bitmessage.ports.MemoryNodeRegistry;
import ch.dissem.bitmessage.ports.MessageRepository;
import ch.dissem.bitmessage.security.sc.SpongySecurity;

/**
 * Provides singleton objects across the application.
 */
public class Singleton {
    public static final Object lock = new Object();
    private static BitmessageContext bitmessageContext;
    private static MessageListener messageListener;

    public static BitmessageContext getBitmessageContext(Context context) {
        if (bitmessageContext == null) {
            synchronized (lock) {
                if (bitmessageContext == null) {
                    final Context ctx = context.getApplicationContext();
                    SqlHelper sqlHelper = new SqlHelper(ctx);
                    bitmessageContext = new BitmessageContext.Builder()
                            .proofOfWorkEngine(new ServicePowEngine(ctx))
                            .security(new AndroidSecurity(ctx))
                            .nodeRegistry(new MemoryNodeRegistry())
                            .inventory(new AndroidInventory(sqlHelper))
                            .addressRepo(new AndroidAddressRepository(sqlHelper))
                            .messageRepo(new AndroidMessageRepository(sqlHelper, ctx))
                            .networkHandler(new DefaultNetworkHandler())
                            .listener(getMessageListener(ctx))
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
}
