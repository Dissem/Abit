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

package ch.dissem.apps.abit.adapter

import android.content.Context
import android.preference.PreferenceManager
import ch.dissem.bitmessage.InternalContext
import ch.dissem.bitmessage.ports.ProofOfWorkEngine

/**
 * Switches between two [ProofOfWorkEngine]s depending on the configuration.
 *
 * @author Christian Basler
 */
class SwitchingProofOfWorkEngine(
        private val ctx: Context,
        private val preference: String,
        private val option: ProofOfWorkEngine,
        private val fallback: ProofOfWorkEngine
) : ProofOfWorkEngine, InternalContext.ContextHolder {

    override fun calculateNonce(initialHash: ByteArray, target: ByteArray, callback: ProofOfWorkEngine.Callback) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)
        if (preferences.getBoolean(preference, false)) {
            option.calculateNonce(initialHash, target, callback)
        } else {
            fallback.calculateNonce(initialHash, target, callback)
        }
    }

    override fun setContext(context: InternalContext) = listOf(option, fallback)
            .filterIsInstance<InternalContext.ContextHolder>()
            .forEach { it.setContext(context) }
}
