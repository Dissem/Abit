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

package ch.dissem.apps.abit.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;

import ch.dissem.apps.abit.notification.NetworkNotification;
import ch.dissem.bitmessage.BitmessageContext;
import ch.dissem.bitmessage.utils.Property;

import static ch.dissem.apps.abit.notification.NetworkNotification.NETWORK_NOTIFICATION_ID;

/**
 * Define a Service that returns an IBinder for the
 * sync adapter class, allowing the sync adapter framework to call
 * onPerformSync().
 */
public class BitmessageService extends Service {
    private static BitmessageContext bmc = null;
    private static volatile boolean running = false;

    private NetworkNotification notification = null;

    private final Handler cleanupHandler = new Handler();
    private final Runnable cleanupTask = new Runnable() {
        @Override
        public void run() {
            bmc.cleanup();
            if (isRunning()) {
                cleanupHandler.postDelayed(this, 24 * 60 * 60 * 1000L);
            }
        }
    };

    public static boolean isRunning() {
        return running && bmc.isRunning();
    }

    @Override
    public void onCreate() {
        if (bmc == null) {
            bmc = Singleton.getBitmessageContext(this);
        }
        notification = new NetworkNotification(this);
        running = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isRunning()) {
            running = true;
            notification.connecting();
            startForeground(NETWORK_NOTIFICATION_ID, notification.getNotification());
            if (!bmc.isRunning()) {
                bmc.startup();
            }
            notification.show();
            cleanupHandler.postDelayed(cleanupTask, 24 * 60 * 60 * 1000L);
        }
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (bmc.isRunning()) {
            bmc.shutdown();
        }
        running = false;
        notification.showShutdown();
        cleanupHandler.removeCallbacks(cleanupTask);
        bmc.cleanup();
        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static Property getStatus() {
        if (bmc != null) {
            return bmc.status();
        } else {
            return new Property("bitmessage context", null);
        }
    }
}
