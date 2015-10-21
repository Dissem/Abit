package ch.dissem.apps.abit.synchronization;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import ch.dissem.apps.abit.MessageListActivity;
import ch.dissem.apps.abit.listener.MessageListener;
import ch.dissem.apps.abit.notification.NetworkNotification;
import ch.dissem.apps.abit.repository.AndroidAddressRepository;
import ch.dissem.apps.abit.repository.AndroidInventory;
import ch.dissem.apps.abit.repository.AndroidMessageRepository;
import ch.dissem.apps.abit.repository.SqlHelper;
import ch.dissem.apps.abit.service.Singleton;
import ch.dissem.bitmessage.BitmessageContext;
import ch.dissem.bitmessage.networking.DefaultNetworkHandler;
import ch.dissem.bitmessage.ports.MemoryNodeRegistry;
import ch.dissem.bitmessage.security.sc.SpongySecurity;

import static ch.dissem.apps.abit.notification.NetworkNotification.ONGOING_NOTIFICATION_ID;

/**
 * Define a Service that returns an IBinder for the
 * sync adapter class, allowing the sync adapter framework to call
 * onPerformSync().
 */
public class SyncService extends Service {
    private static MessageListener messageListener = null;
    private static BitmessageContext bmc = null;
    // Storage for an instance of the sync adapter
    private static SyncAdapter syncAdapter = null;
    // Object to use as a thread-safe lock
    private static final Object syncAdapterLock = new Object();

    private static volatile boolean running = false;

    public static boolean isRunning() {
        return running;
    }

    /*
     * Instantiate the sync adapter object.
     */
    @Override
    public void onCreate() {
        /*
         * Create the sync adapter as a singleton.
         * Set the sync adapter as syncable
         * Disallow parallel syncs
         */
        synchronized (syncAdapterLock) {
            final Context ctx = getApplicationContext();
            if (bmc == null) {
//                messageListener = new MessageListener(ctx);
//                SqlHelper sqlHelper = new SqlHelper(ctx);
//                bmc = new BitmessageContext.Builder()
//                        .security(new SpongySecurity())
//                        .nodeRegistry(new MemoryNodeRegistry())
//                        .inventory(new AndroidInventory(sqlHelper))
//                        .addressRepo(new AndroidAddressRepository(sqlHelper))
//                        .messageRepo(new AndroidMessageRepository(sqlHelper, ctx))
//                        .networkHandler(new DefaultNetworkHandler())
//                        .listener(messageListener)
//                        .build();
                // FIXME: this needs to change once I figured out how to get rid of those singletons
                messageListener = Singleton.getMessageListener(ctx);
                bmc = Singleton.getBitmessageContext(ctx);
            }
            if (syncAdapter == null) {
                syncAdapter = new SyncAdapter(ctx, bmc);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO: warn user, option to restrict to WiFi
        running = true;
        NetworkNotification networkNotification = new NetworkNotification(this);
        startForeground(ONGOING_NOTIFICATION_ID, networkNotification.getNotification());
        bmc.startup();
        networkNotification.show();
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        bmc.shutdown();
        running = false;
    }

    /**
     * Return an object that allows the system to invoke
     * the sync adapter.
     */
    @Override
    public IBinder onBind(Intent intent) {
        /*
         * Get the object that allows external processes
         * to call onPerformSync(). The object is created
         * in the base class code when the SyncAdapter
         * constructors call super()
         */
        return syncAdapter.getSyncAdapterBinder();
    }
}