package ch.dissem.apps.abit.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;
import ch.dissem.apps.abit.R;
import ch.dissem.bitmessage.BitmessageContext;
import ch.dissem.bitmessage.entity.BitmessageAddress;
import ch.dissem.bitmessage.entity.Plaintext;
import ch.dissem.bitmessage.entity.valueobject.Label;

import java.util.List;

public class BitmessageService extends Service {
    private static BitmessageContext ctx;
    private ServiceBinder binder = new ServiceBinder();
    private NotificationCompat.Builder ongoingNotificationBuilder = new NotificationCompat.Builder(this);
    private NotificationManager notifyManager;

    public BitmessageService() {
        if (ctx == null) {
            ctx = Singleton.getBitmessageContext(this);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ongoingNotificationBuilder.setOngoing(true);
        ongoingNotificationBuilder.setContentTitle(getString(R.string.bitmessage_active));
        ongoingNotificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
//        ongoingNotificationBuilder.setSmallIcon(R.drawable.ic_bitmessage);
        notifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public void startService() {
        if (!ctx.isRunning()) {
            notifyManager.notify(0, ongoingNotificationBuilder.build());
            ctx.startup(new BitmessageContext.Listener() {
                @Override
                public void receive(Plaintext plaintext) {
                    Notification notification = new NotificationCompat.Builder(BitmessageService.this)
                            .setContentTitle(plaintext.getSubject())
                            .setContentText(plaintext.getText())
                            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                            .setPriority(plaintext.getType() == Plaintext.Type.BROADCAST
                                    ? NotificationCompat.PRIORITY_DEFAULT
                                    : NotificationCompat.PRIORITY_HIGH)
                            .build();
                    notifyManager.notify(plaintext.getInventoryVector().hashCode(), notification);
                }
            });
        }
    }

    public void stopService() {
        ctx.shutdown();
        notifyManager.cancel(0);
    }

    public List<BitmessageAddress> getIdentities() {
        return ctx.addresses().getIdentities();
    }

    public List<BitmessageAddress> getContacts() {
        return ctx.addresses().getContacts();
    }

    public List<Plaintext> getMessages(Label label) {
        return ctx.messages().findMessages(label);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class ServiceBinder extends Binder {
        public BitmessageService getService() {
            return BitmessageService.this;
        }
    }

    public enum NetworkChoice {
        /**
         * A full node, receiving and relaying objects all the time.
         */
        FULL,
        /**
         * Connect to a trusted node from time to time to get all new objects and disconnect afterwards
         * (see {@link android.content.AbstractThreadedSyncAdapter})
         */
        TRUSTED,
        /**
         * Offline
         */
        NONE
    }
}
