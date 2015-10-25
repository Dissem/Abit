package ch.dissem.apps.abit.synchronization;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import ch.dissem.apps.abit.listener.MessageListener;
import ch.dissem.apps.abit.notification.NetworkNotification;
import ch.dissem.apps.abit.repository.AndroidInventory;
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
    // Storage for an instance of the sync adapter
    private static SyncAdapter syncAdapter = null;
    // Object to use as a thread-safe lock
    private static final Object syncAdapterLock = new Object();

    /**
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
            if (syncAdapter == null) {
                syncAdapter = new SyncAdapter(this, true);
            }
        }
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