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
import android.support.annotation.Nullable;

import java.util.LinkedList;
import java.util.Queue;

import ch.dissem.apps.abit.notification.ProofOfWorkNotification;
import ch.dissem.bitmessage.ports.MultiThreadedPOWEngine;
import ch.dissem.bitmessage.ports.ProofOfWorkEngine;

import static ch.dissem.apps.abit.notification.ProofOfWorkNotification.ONGOING_NOTIFICATION_ID;

/**
 * The Proof of Work Service makes sure POW is done in a foreground process, so it shouldn't be
 * killed by the system before the nonce is found.
 */
public class ProofOfWorkService extends Service {
    // Object to use as a thread-safe lock
    private static final ProofOfWorkEngine engine = new MultiThreadedPOWEngine();
    private static final Queue<PowItem> queue = new LinkedList<>();
    private static boolean calculating;
    private ProofOfWorkNotification notification;

    @Override
    public void onCreate() {
        notification = new ProofOfWorkNotification(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new PowBinder(this);
    }

    public static class PowBinder extends Binder {
        private final ProofOfWorkService service;
        private final ProofOfWorkNotification notification;

        private PowBinder(ProofOfWorkService service) {
            this.service = service;
            this.notification = service.notification;
        }

        void process(PowItem item) {
            synchronized (queue) {
                service.startService(new Intent(service, ProofOfWorkService.class));
                service.startForeground(ONGOING_NOTIFICATION_ID,
                    notification.getNotification());
                if (!calculating) {
                    calculating = true;
                    service.calculateNonce(item);
                } else {
                    queue.add(item);
                    notification.update(queue.size()).show();
                }
            }
        }
    }


    static class PowItem {
        private final byte[] initialHash;
        private final byte[] targetValue;
        private final ProofOfWorkEngine.Callback callback;

        PowItem(byte[] initialHash, byte[] targetValue, ProofOfWorkEngine.Callback callback) {
            this.initialHash = initialHash;
            this.targetValue = targetValue;
            this.callback = callback;
        }
    }

    private void calculateNonce(final PowItem item) {
        engine.calculateNonce(item.initialHash, item.targetValue, new ProofOfWorkEngine.Callback() {
            @Override
            public void onNonceCalculated(byte[] initialHash, byte[] nonce) {
                try {
                    item.callback.onNonceCalculated(initialHash, nonce);
                } finally {
                    PowItem next;
                    synchronized (queue) {
                        next = queue.poll();
                        if (next == null) {
                            calculating = false;
                            stopForeground(true);
                            stopSelf();
                        } else {
                            notification.update(queue.size()).show();
                        }
                    }
                    if (next != null) {
                        calculateNonce(next);
                    }
                }
            }
        });
    }
}
