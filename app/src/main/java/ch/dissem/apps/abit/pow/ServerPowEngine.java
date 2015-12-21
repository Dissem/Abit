package ch.dissem.apps.abit.pow;

import android.content.Context;
import android.support.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import ch.dissem.apps.abit.service.Singleton;
import ch.dissem.apps.abit.synchronization.SyncAdapter;
import ch.dissem.apps.abit.util.Preferences;
import ch.dissem.bitmessage.InternalContext;
import ch.dissem.bitmessage.entity.BitmessageAddress;
import ch.dissem.bitmessage.extensions.CryptoCustomMessage;
import ch.dissem.bitmessage.extensions.pow.ProofOfWorkRequest;
import ch.dissem.bitmessage.ports.ProofOfWorkEngine;

import static ch.dissem.bitmessage.extensions.pow.ProofOfWorkRequest.Request.CALCULATE;
import static ch.dissem.bitmessage.utils.Singleton.security;

/**
 * @author Christian Basler
 */
public class ServerPowEngine implements ProofOfWorkEngine, InternalContext
        .ContextHolder {
    private static final Logger LOG = LoggerFactory.getLogger(ServerPowEngine.class);

    private final Context ctx;
    private InternalContext context;

    private final ExecutorService pool;

    public ServerPowEngine(Context ctx) {
        this.ctx = ctx;
        pool = Executors.newCachedThreadPool(new ThreadFactory() {
            @Override
            public Thread newThread(@NonNull Runnable r) {
                Thread thread = Executors.defaultThreadFactory().newThread(r);
                thread.setPriority(Thread.MIN_PRIORITY);
                return thread;
            }
        });
    }

    @Override
    public void calculateNonce(final byte[] initialHash, final byte[] target, Callback callback) {
        pool.execute(new Runnable() {
            @Override
            public void run() {
                BitmessageAddress identity = Singleton.getIdentity(ctx);
                if (identity == null) throw new RuntimeException("No Identity for calculating POW");

                ProofOfWorkRequest request = new ProofOfWorkRequest(identity, initialHash,
                        CALCULATE, target);
                SyncAdapter.startPowSync(ctx);
                try {
                    CryptoCustomMessage<ProofOfWorkRequest> cryptoMsg = new CryptoCustomMessage<>
                            (request);
                    cryptoMsg.signAndEncrypt(
                            identity,
                            security().createPublicKey(identity.getPublicDecryptionKey())
                    );
                    context.getNetworkHandler().send(
                            Preferences.getTrustedNode(ctx), Preferences.getTrustedNodePort(ctx),
                            cryptoMsg);
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        });
    }

    @Override
    public void setContext(InternalContext context) {
        this.context = context;
    }
}
