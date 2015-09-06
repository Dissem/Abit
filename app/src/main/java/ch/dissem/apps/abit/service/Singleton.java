package ch.dissem.apps.abit.service;

import android.content.Context;
import ch.dissem.apps.abit.listeners.MessageListener;
import ch.dissem.apps.abit.repositories.AndroidAddressRepository;
import ch.dissem.apps.abit.repositories.AndroidInventory;
import ch.dissem.apps.abit.repositories.AndroidMessageRepository;
import ch.dissem.apps.abit.repositories.SqlHelper;
import ch.dissem.bitmessage.BitmessageContext;
import ch.dissem.bitmessage.networking.DefaultNetworkHandler;
import ch.dissem.bitmessage.ports.MemoryNodeRegistry;
import ch.dissem.bitmessage.security.sc.SpongySecurity;

/**
 * Provides singleton objects across the application.
 */
public class Singleton {
    private static BitmessageContext bitmessageContext;
    private static MessageListener messageListener;

    public static BitmessageContext getBitmessageContext(Context ctx) {
        if (bitmessageContext == null) {
            synchronized (Singleton.class) {
                if (bitmessageContext == null) {
                    ctx = ctx.getApplicationContext();
                    SqlHelper sqlHelper = new SqlHelper(ctx);
                    bitmessageContext = new BitmessageContext.Builder()
                            .security(new SpongySecurity())
                            .nodeRegistry(new MemoryNodeRegistry())
                            .inventory(new AndroidInventory(sqlHelper))
                            .addressRepo(new AndroidAddressRepository(sqlHelper))
                            .messageRepo(new AndroidMessageRepository(sqlHelper, ctx))
                            .networkHandler(new DefaultNetworkHandler())
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
}
