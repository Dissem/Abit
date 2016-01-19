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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;

import ch.dissem.apps.abit.notification.ProofOfWorkNotification;
import ch.dissem.bitmessage.ports.MultiThreadedPOWEngine;
import ch.dissem.bitmessage.ports.ProofOfWorkEngine;

import static ch.dissem.apps.abit.notification.ProofOfWorkNotification.ONGOING_NOTIFICATION_ID;

/**
 * The Proof of Work Service makes sure POW is done in a foreground process, so it shouldn't be
 * killed by the system before the nonce is found.
 */
public class ProofOfWorkService extends Service {
    public static final Logger LOG = LoggerFactory.getLogger(ProofOfWorkService.class);

    // Object to use as a thread-safe lock
    private static final Object lock = new Object();
    private static ProofOfWorkEngine engine;

    @Override
    public void onCreate() {
        synchronized (lock) {
            if (engine == null) {
                engine = new MultiThreadedPOWEngine();
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new PowBinder(engine, this);
    }

    public static class PowBinder extends Binder {
        private final ProofOfWorkEngine engine;

        private PowBinder(ProofOfWorkEngine engine, ProofOfWorkService service) {
            this.engine = new EngineWrapper(engine, service);
        }

        public ProofOfWorkEngine getEngine() {
            return engine;
        }
    }

    private static class EngineWrapper implements ProofOfWorkEngine {
        private final ProofOfWorkNotification notification;
        private final ProofOfWorkEngine engine;
        private final WeakReference<ProofOfWorkService> serviceRef;

        private EngineWrapper(ProofOfWorkEngine engine, ProofOfWorkService service) {
            this.engine = engine;
            this.serviceRef = new WeakReference<>(service);
            this.notification = new ProofOfWorkNotification(service);
        }

        @Override
        public void calculateNonce(byte[] initialHash, byte[] target, final Callback callback) {
            final ProofOfWorkService service = serviceRef.get();
            service.startService(new Intent(service, ProofOfWorkService.class));
            service.startForeground(ONGOING_NOTIFICATION_ID, notification.getNotification());
            engine.calculateNonce(initialHash, target, new ProofOfWorkEngine.Callback() {
                @Override
                public void onNonceCalculated(byte[] initialHash, byte[] nonce) {
                    try {
                        callback.onNonceCalculated(initialHash, nonce);
                    } finally {
                        service.stopForeground(true);
                        service.stopSelf();
                    }
                }
            });

        }
    }
}