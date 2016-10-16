/*
 * Copyright 2016 Christian Basler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.dissem.apps.abit.notification;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.NotificationCompat;

import java.util.Timer;
import java.util.TimerTask;

import ch.dissem.apps.abit.MainActivity;
import ch.dissem.apps.abit.R;
import ch.dissem.apps.abit.service.BitmessageService;
import ch.dissem.bitmessage.utils.Property;

/**
 * Shows the network status (as long as the client is connected as a full node)
 */
public class NetworkNotification extends AbstractNotification {
    public static final int ONGOING_NOTIFICATION_ID = 2;

    private NotificationCompat.Builder builder;

    public NetworkNotification(Context ctx) {
        super(ctx);
        Intent showMessageIntent = new Intent(ctx, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(ctx, 1, showMessageIntent, 0);
        builder = new NotificationCompat.Builder(ctx);
        builder.setSmallIcon(R.drawable.ic_notification_full_node)
            .setContentTitle(ctx.getString(R.string.bitmessage_full_node))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(false)
            .setContentIntent(pendingIntent);
    }

    @SuppressLint("StringFormatMatches")
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean update() {
        boolean running = BitmessageService.isRunning();
        builder.setOngoing(running);
        Property connections = BitmessageService.getStatus().getProperty("network", "connections");
        if (!running) {
            builder.setContentText(ctx.getString(R.string.connection_info_disconnected));
            MainActivity mainActivity = MainActivity.getInstance();
            if (mainActivity != null) {
                mainActivity.updateNodeSwitch();
            }
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
        notification = builder.build();
        return running;
    }

    @Override
    public void show() {
        super.show();

        new Timer().schedule(new TimerTask() {
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
        return ONGOING_NOTIFICATION_ID;
    }

    public void connecting() {
        builder.setOngoing(true);
        builder.setContentText(ctx.getString(R.string.connection_info_pending));
        notification = builder.build();
    }
}
