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
import android.os.Binder;
import android.os.IBinder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.dissem.apps.abit.notification.NetworkNotification;
import ch.dissem.bitmessage.BitmessageContext;

import static ch.dissem.apps.abit.notification.NetworkNotification.ONGOING_NOTIFICATION_ID;

/**
 * Define a Service that returns an IBinder for the
 * sync adapter class, allowing the sync adapter framework to call
 * onPerformSync().
 */
public class BitmessageService extends Service {
    public static final Logger LOG = LoggerFactory.getLogger(BitmessageService.class);

    // Object to use as a thread-safe lock
    private static final Object lock = new Object();

    private static NetworkNotification notification = null;
    private static BitmessageContext bmc = null;

    private static volatile boolean running = false;

    public static boolean isRunning() {
        return running && bmc.isRunning();
    }

    @Override
    public void onCreate() {
        synchronized (lock) {
            if (bmc == null) {
                bmc = Singleton.getBitmessageContext(this);
                notification = new NetworkNotification(this, bmc);
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
        return new BitmessageBinder();
    }

    public class BitmessageBinder extends Binder {
        public void startupNode() {
            startService(new Intent(BitmessageService.this, BitmessageService.class));
            running = true;
            startForeground(ONGOING_NOTIFICATION_ID, notification.getNotification());
            if (!bmc.isRunning()) {
                bmc.startup();
            }
            notification.show();
        }

        public void shutdownNode() {
            if (bmc.isRunning()) {
                bmc.shutdown();
            }
            running = false;
            stopForeground(false);
            stopSelf();
        }
    }
}