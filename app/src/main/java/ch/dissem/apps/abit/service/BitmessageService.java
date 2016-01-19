package ch.dissem.apps.abit.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.ref.WeakReference;

import ch.dissem.apps.abit.R;
import ch.dissem.apps.abit.notification.NetworkNotification;
import ch.dissem.bitmessage.BitmessageContext;
import ch.dissem.bitmessage.entity.BitmessageAddress;

import static ch.dissem.apps.abit.notification.NetworkNotification.ONGOING_NOTIFICATION_ID;

/**
 * Define a Service that returns an IBinder for the
 * sync adapter class, allowing the sync adapter framework to call
 * onPerformSync().
 */
public class BitmessageService extends Service {
    public static final Logger LOG = LoggerFactory.getLogger(BitmessageService.class);

    public static final int MSG_CREATE_IDENTITY = 10;
    public static final int MSG_SUBSCRIBE = 20;
    public static final int MSG_ADD_CONTACT = 21;
    public static final int MSG_SEND_MESSAGE = 30;
    public static final int MSG_SEND_BROADCAST = 31;
    public static final int MSG_START_NODE = 100;
    public static final int MSG_STOP_NODE = 101;

    public static final String DATA_FIELD_IDENTITY = "identity";
    public static final String DATA_FIELD_ADDRESS = "address";
    public static final String DATA_FIELD_SUBJECT = "subject";
    public static final String DATA_FIELD_MESSAGE = "message";

    // Object to use as a thread-safe lock
    private static final Object lock = new Object();

    private static NetworkNotification notification = null;
    private static BitmessageContext bmc = null;

    private static volatile boolean running = false;

    private static Messenger messenger;

    public static boolean isRunning() {
        return running && bmc.isRunning();
    }

    @Override
    public void onCreate() {
        synchronized (lock) {
            if (bmc == null) {
                bmc = Singleton.getBitmessageContext(this);
                notification = new NetworkNotification(this, bmc);
                messenger = new Messenger(new IncomingHandler(this));
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

    private static class IncomingHandler extends Handler {
        private WeakReference<BitmessageService> service;

        private IncomingHandler(BitmessageService service) {
            this.service = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CREATE_IDENTITY: {
                    BitmessageAddress identity = bmc.createIdentity(false);
                    if (msg.replyTo != null) {
                        try {
                            Message message = Message.obtain(this, MSG_CREATE_IDENTITY);
                            Bundle bundle = new Bundle();
                            bundle.putSerializable(DATA_FIELD_IDENTITY, identity);
                            message.setData(bundle);
                            msg.replyTo.send(message);
                        } catch (RemoteException e) {
                            LOG.debug(e.getMessage(), e);
                        }
                    }
                    break;
                }
                case MSG_SUBSCRIBE: {
                    Serializable data = msg.getData().getSerializable(DATA_FIELD_ADDRESS);
                    if (data instanceof BitmessageAddress) {
                        bmc.addSubscribtion((BitmessageAddress) data);
                    }
                    break;
                }
                case MSG_ADD_CONTACT: {
                    Serializable data = msg.getData().getSerializable(DATA_FIELD_ADDRESS);
                    if (data instanceof BitmessageAddress) {
                        bmc.addContact((BitmessageAddress) data);
                    }
                    break;
                }
                case MSG_SEND_MESSAGE: {
                    Serializable identity = msg.getData().getSerializable(DATA_FIELD_IDENTITY);
                    Serializable address = msg.getData().getSerializable(DATA_FIELD_ADDRESS);
                    if (identity instanceof BitmessageAddress
                            && address instanceof BitmessageAddress) {
                        String subject = msg.getData().getString(DATA_FIELD_SUBJECT);
                        String message = msg.getData().getString(DATA_FIELD_MESSAGE);
                        bmc.send((BitmessageAddress) identity, (BitmessageAddress) address,
                                subject, message);
                    } else {
                        Context ctx = service.get();
                        Toast.makeText(ctx, "Could not send", Toast.LENGTH_LONG);
                    }
                    break;
                }
                case MSG_SEND_BROADCAST: {
                    Serializable data = msg.getData().getSerializable(DATA_FIELD_IDENTITY);
                    if (data instanceof BitmessageAddress) {
                        String subject = msg.getData().getString(DATA_FIELD_SUBJECT);
                        String message = msg.getData().getString(DATA_FIELD_MESSAGE);
                        bmc.broadcast((BitmessageAddress) data, subject, message);
                    }
                    break;
                }
                case MSG_START_NODE:
                    // TODO: warn user, option to restrict to WiFi
                    // (I'm not quite sure this can be done here, though)
                    service.get().startService(new Intent(service.get(), BitmessageService.class));
                    running = true;
                    service.get().startForeground(ONGOING_NOTIFICATION_ID, notification
                            .getNotification());
                    if (!bmc.isRunning()) {
                        bmc.startup();
                    }
                    notification.show();
                    break;
                case MSG_STOP_NODE:
                    if (bmc.isRunning()) {
                        bmc.shutdown();
                    }
                    running = false;
                    service.get().stopForeground(false);
                    service.get().stopSelf();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}