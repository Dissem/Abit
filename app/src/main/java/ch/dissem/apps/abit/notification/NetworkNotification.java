package ch.dissem.apps.abit.notification;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.NotificationCompat;

import java.util.Timer;
import java.util.TimerTask;

import ch.dissem.apps.abit.MessageListActivity;
import ch.dissem.apps.abit.R;
import ch.dissem.apps.abit.service.Singleton;
import ch.dissem.bitmessage.BitmessageContext;
import ch.dissem.bitmessage.utils.Property;

/**
 * Shows the network status (as long as the client is connected as a full node)
 */
public class NetworkNotification extends AbstractNotification {
    private final BitmessageContext bmc;
    private NotificationCompat.Builder builder;

    public NetworkNotification(Context ctx) {
        super(ctx);
        bmc = Singleton.getBitmessageContext(ctx);
        builder = new NotificationCompat.Builder(ctx);
        builder.setSmallIcon(R.drawable.ic_notification_full_node)
                .setContentTitle(ctx.getString(R.string.bitmessage_active))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
    }

    @SuppressLint("StringFormatMatches")
    private boolean update() {
        boolean running = bmc.isRunning();
        builder.setOngoing(running);
        Property connections = bmc.status().getProperty("network").getProperty("connections");
        if (!running) {
            builder.setContentText(ctx.getString(R.string.connection_info_disconnected));
        } else if (connections.getProperties().length == 0) {
            builder.setContentText(ctx.getString(R.string.connection_info_pending));
        } else {
            StringBuilder info = new StringBuilder();
            for (Property stream : connections.getProperties()) {
                int streamNumber = Integer.parseInt(stream.getName().substring("stream ".length()));
                Integer nodeCount = (Integer) stream.getProperty("nodes").getValue();
                if (nodeCount == 1) {
                    info.append(ctx.getString(R.string.connection_info_1,
                            streamNumber));
                } else {
                    info.append(ctx.getString(R.string.connection_info_n,
                            streamNumber, nodeCount));
                }
                info.append('\n');
            }
            builder.setContentText(info);
        }
        Intent showMessageIntent = new Intent(ctx, MessageListActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(ctx, 1, showMessageIntent, 0);
        builder.setContentIntent(pendingIntent);
        notification = builder.build();
        return running;
    }

    @Override
    public void show() {
        update();
        super.show();

        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!update()) {
                    cancel();
                }
                NetworkNotification.super.show();
            }
        }, 10_000, 10_000);
    }

    @Override
    protected int getNotificationId() {
        return 2;
    }
}
