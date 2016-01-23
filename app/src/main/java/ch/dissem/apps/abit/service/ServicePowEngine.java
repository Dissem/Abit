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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import java.util.LinkedList;
import java.util.Queue;

import ch.dissem.apps.abit.service.ProofOfWorkService.PowBinder;
import ch.dissem.apps.abit.service.ProofOfWorkService.PowItem;
import ch.dissem.bitmessage.ports.ProofOfWorkEngine;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * Proof of Work engine that uses the Proof of Work service.
 */
public class ServicePowEngine implements ProofOfWorkEngine {
    private final Context ctx;

    private static final Object lock = new Object();
    private Queue<PowItem> queue = new LinkedList<>();
    private PowBinder service;

    public ServicePowEngine(Context ctx) {
        this.ctx = ctx;
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (lock) {
                ServicePowEngine.this.service = (PowBinder) service;
                while (!queue.isEmpty()) {
                    ServicePowEngine.this.service.process(queue.poll());
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            service = null;
        }
    };

    @Override
    public void calculateNonce(byte[] initialHash, byte[] targetValue, Callback callback) {
        PowItem item = new PowItem(initialHash, targetValue, callback);
        synchronized (lock) {
            if (service != null) {
                service.process(item);
            } else {
                queue.add(item);
                ctx.bindService(new Intent(ctx, ProofOfWorkService.class), connection,
                        BIND_AUTO_CREATE);
            }
        }
    }
}