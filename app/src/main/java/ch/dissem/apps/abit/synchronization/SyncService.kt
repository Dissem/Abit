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

package ch.dissem.apps.abit.synchronization

import android.app.Service
import android.content.Intent

/**
 * Define a Service that returns an IBinder for the
 * sync adapter class, allowing the sync adapter framework to call
 * onPerformSync().
 */
class SyncService : Service() {

    /**
     * Instantiate the sync adapter object.
     */
    override fun onCreate() = synchronized(syncAdapterLock) {
        if (syncAdapter == null) {
            syncAdapter = SyncAdapter(this, true)
        }
    }

    /**
     * Return an object that allows the system to invoke
     * the sync adapter.
     */
    override fun onBind(intent: Intent) = syncAdapter?.syncAdapterBinder

    companion object {
        // Storage for an instance of the sync adapter
        private var syncAdapter: SyncAdapter? = null
        // Object to use as a thread-safe lock
        private val syncAdapterLock = Any()
    }
}
