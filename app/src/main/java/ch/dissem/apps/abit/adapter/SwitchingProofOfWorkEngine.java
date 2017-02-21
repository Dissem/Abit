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

package ch.dissem.apps.abit.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Arrays;

import ch.dissem.bitmessage.InternalContext;
import ch.dissem.bitmessage.ports.ProofOfWorkEngine;

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
