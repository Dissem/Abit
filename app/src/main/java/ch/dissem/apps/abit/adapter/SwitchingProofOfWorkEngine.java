package ch.dissem.apps.abit.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Arrays;

import ch.dissem.apps.abit.util.Preferences;
import ch.dissem.bitmessage.InternalContext;
import ch.dissem.bitmessage.ports.ProofOfWorkEngine;

import static ch.dissem.apps.abit.util.Constants.PREFERENCE_SERVER_POW;

/**
 * Switches between two {@link ProofOfWorkEngine}s depending on the configuration.
 *
 * @author Christian Basler
 */
public class SwitchingProofOfWorkEngine implements ProofOfWorkEngine, InternalContext.ContextHolder {
    private final Context ctx;
    private final String preference;
    private final ProofOfWorkEngine option;
    private final ProofOfWorkEngine fallback;

    public SwitchingProofOfWorkEngine(Context ctx, String preference,
                                      ProofOfWorkEngine option, ProofOfWorkEngine fallback) {
        this.ctx = ctx;
        this.preference = preference;
        this.option = option;
        this.fallback = fallback;
    }

    @Override
    public void calculateNonce(byte[] initialHash, byte[] target, Callback callback) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
        if (preferences.getBoolean(preference, false)) {
            option.calculateNonce(initialHash, target, callback);
        } else {
            fallback.calculateNonce(initialHash, target, callback);
        }
    }

    @Override
    public void setContext(InternalContext context) {
        for (ProofOfWorkEngine e : Arrays.asList(option, fallback)) {
            if (e instanceof InternalContext.ContextHolder) {
                ((InternalContext.ContextHolder) e).setContext(context);
            }
        }
    }
}
