package ch.dissem.apps.abit.service;

import android.content.Context;
import ch.dissem.apps.abit.SQLiteConfig;
import ch.dissem.bitmessage.BitmessageContext;
import ch.dissem.bitmessage.networking.DefaultNetworkHandler;
import ch.dissem.bitmessage.repository.*;
import ch.dissem.bitmessage.security.sc.SpongySecurity;

/**
 * Created by chris on 16.07.15.
 */
public class Singleton {
    private static BitmessageContext bitmessageContext;

    public static BitmessageContext getBitmessageContext(Context ctx) {
        if (bitmessageContext == null) {
            synchronized (Singleton.class) {
                if (bitmessageContext == null) {
                    JdbcConfig config = new SQLiteConfig(ctx);
                    bitmessageContext = new BitmessageContext.Builder()
                            .security(new SpongySecurity())
                            .nodeRegistry(new MemoryNodeRegistry())
                            .inventory(new JdbcInventory(config))
                            .addressRepo(new JdbcAddressRepository(config))
                            .messageRepo(new JdbcMessageRepository(config))
                            .networkHandler(new DefaultNetworkHandler())
                            .build();
                }
            }
        }
        return bitmessageContext;
    }
}
