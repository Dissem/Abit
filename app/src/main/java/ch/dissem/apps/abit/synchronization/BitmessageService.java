package ch.dissem.apps.abit.synchronization;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

import ch.dissem.apps.abit.listener.MessageListener;
import ch.dissem.apps.abit.notification.NetworkNotification;
import ch.dissem.apps.abit.repository.AndroidInventory;
import ch.dissem.apps.abit.repository.SqlHelper;
import ch.dissem.apps.abit.service.Singleton;
import ch.dissem.bitmessage.BitmessageContext;
import ch.dissem.bitmessage.entity.BitmessageAddress;
import ch.dissem.bitmessage.networking.DefaultNetworkHandler;
import ch.dissem.bitmessage.ports.MemoryNodeRegistry;
import ch.dissem.bitmessage.security.sc.SpongySecurity;

import static ch.dissem.apps.abit.notification.NetworkNotification.ONGOING_NOTIFICATION_ID;

/**
 * Define a Service that returns an IBinder for the
 * sync adapter class, allowing the sync adapter framework to call
 * onPerformSync().
 */
public class BitmessageService extends Service {
    public static final Logger LOG = LoggerFactory.getLogger(BitmessageService.class);

    public static final int MSG_SYNC = 2;
    public static final int MSG_CREATE_IDENTITY = 10;
    public static final int MSG_SUBSCRIBE = 20;
    public static final int MSG_ADD_CONTACT = 21;
    public static final int MSG_SUBSCRIBE_AND_ADD_CONTACT = 23;
    public static final int MSG_START_NODE = 100;
    public static final int MSG_STOP_NODE = 101;

    public static final String DATA_FIELD_ADDRESS = "address";

    // Object to use as a thread-safe lock
    private static final Object lock = new Object();

    private static MessageListener messageListener = null;
    private static NetworkNotification notification = null;
    private static BitmessageContext bmc = null;

    private static volatile boolean running = false;

    private static Messenger messenger;

    public static boolean isRunning() {
        return running;
    }

    @Override
    public void onCreate() {
        synchronized (lock) {
            if (bmc == null) {
                messageListener = Singleton.getMessageListener(this);
                SqlHelper sqlHelper = Singleton.getSqlHelper(this);
                bmc = new BitmessageContext.Builder()
                        .security(new SpongySecurity())
                        .nodeRegistry(new MemoryNodeRegistry())
                        .inventory(new AndroidInventory(sqlHelper))
                        .addressRepo(Singleton.getAddressRepository(this))
                        .messageRepo(Singleton.getMessageRepository(this))
                        .networkHandler(new DefaultNetworkHandler())
                        .listener(messageListener)
                        .build();
                notification = new NetworkNotification(this, bmc);
                messenger = new Messenger(new IncomingHandler());
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (bmc.isRunning()) bmc.shutdown();
        running = false;
    }

    /**
     * Return an object that allows the system to invoke
     * the sync adapter.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CREATE_IDENTITY:
                    BitmessageAddress identity = bmc.createIdentity(false);
                    if (msg.replyTo != null) {
                        try {
                            Message message = Message.obtain(this, MSG_CREATE_IDENTITY);
                            Bundle bundle = new Bundle();
                            bundle.putSerializable(DATA_FIELD_ADDRESS, identity);
                            message.setData(bundle);
                            msg.replyTo.send(message);
                        } catch (RemoteException e) {
                            LOG.debug(e.getMessage(), e);
                        }
                    }
                    break;
                case MSG_SUBSCRIBE:
                    BitmessageAddress address = (BitmessageAddress) msg.getData().getSerializable(DATA_FIELD_ADDRESS);
                    bmc.addSubscribtion(address);
                    break;
                case MSG_SYNC:
                    LOG.info("Synchronizing Bitmessage");
                    // If the Bitmessage context acts as a full node, synchronization isn't necessary
                    if (bmc.isRunning()) break;

                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(
                            BitmessageService.this);

                    String trustedNode = preferences.getString("trusted_node", null);
                    if (trustedNode == null) break;
                    trustedNode = trustedNode.trim();
                    if (trustedNode.isEmpty()) break;

                    int port;
                    if (trustedNode.matches("^(?![0-9a-fA-F]*:[0-9a-fA-F]*:).*(:[0-9]+)$")) {
                        int index = trustedNode.lastIndexOf(':');
                        String portString = trustedNode.substring(index + 1);
                        trustedNode = trustedNode.substring(0, index);
                        try {
                            port = Integer.parseInt(portString);
                        } catch (NumberFormatException e) {
                            LOG.error("Invalid port " + portString);
                            // TODO: show error as notification
                            return;
                        }
                    } else {
                        port = 8444;
                    }
                    long timeoutInSeconds = preferences.getInt("sync_timeout", 120);
                    try {
                        LOG.info("Synchronization started");
                        bmc.synchronize(InetAddress.getByName(trustedNode), port, timeoutInSeconds, true);
                        LOG.info("Synchronization finished");
                    } catch (UnknownHostException e) {
                        LOG.error("Couldn't synchronize", e);
                        // TODO: show error as notification
                    }
                    break;
                case MSG_START_NODE:
                    startService(new Intent(BitmessageService.this, BitmessageService.class));
                    // TODO: warn user, option to restrict to WiFi
                    running = true;
                    startForeground(ONGOING_NOTIFICATION_ID, notification.getNotification());
                    bmc.startup();
                    notification.show();
                    break;
                case MSG_STOP_NODE:
                    bmc.shutdown();
                    running = false;
                    stopForeground(false);
                    stopService(new Intent(BitmessageService.this, BitmessageService.class));
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}