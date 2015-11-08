package ch.dissem.apps.abit.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import java.util.concurrent.Semaphore;

import ch.dissem.apps.abit.service.ProofOfWorkService.PowBinder;
import ch.dissem.bitmessage.ports.ProofOfWorkEngine;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * Proof of Work engine that uses the Proof of Work service.
 */
public class ServicePowEngine implements ProofOfWorkEngine, ProofOfWorkEngine.Callback {
    private final Semaphore semaphore = new Semaphore(1, true);
    private final Context ctx;

    private byte[] initialHash, targetValue;
    private Callback callback;

    public ServicePowEngine(Context ctx) {
        this.ctx = ctx;
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ((PowBinder) service).getEngine().calculateNonce(initialHash, targetValue, ServicePowEngine.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            semaphore.release();
        }
    };

    @Override
    public void calculateNonce(byte[] initialHash, byte[] targetValue, Callback callback) {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        this.initialHash = initialHash;
        this.targetValue = targetValue;
        this.callback = callback;
        ctx.bindService(new Intent(ctx, ProofOfWorkService.class), connection, BIND_AUTO_CREATE);
    }

    @Override
    public void onNonceCalculated(byte[] bytes) {
        callback.onNonceCalculated(bytes);
        ctx.unbindService(connection);
    }

}
