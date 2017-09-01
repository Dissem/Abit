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

package ch.dissem.apps.abit.service

import android.app.IntentService
import android.content.Intent
import ch.dissem.apps.abit.util.NetworkUtils
import ch.dissem.bitmessage.BitmessageContext
import ch.dissem.bitmessage.entity.Plaintext

/**
 * @author Christian Basler
 */

class BitmessageIntentService : IntentService("BitmessageIntentService") {

    private lateinit var bmc: BitmessageContext

    override fun onCreate() {
        super.onCreate()
        bmc = Singleton.getBitmessageContext(this)
    }

    override fun onHandleIntent(intent: Intent?) {
        intent?.let {
            if (it.hasExtra(EXTRA_DELETE_MESSAGE)) {
                val item = it.getSerializableExtra(EXTRA_DELETE_MESSAGE) as Plaintext
                bmc.labeler.delete(item)
                bmc.messages.save(item)
                Singleton.getMessageListener(this).resetNotification()
            }
            if (it.hasExtra(EXTRA_STARTUP_NODE)) {
                NetworkUtils.enableNode(this)
            }
            if (it.hasExtra(EXTRA_SHUTDOWN_NODE)) {
                NetworkUtils.disableNode(this)
            }
        }
    }

    companion object {
        const val EXTRA_DELETE_MESSAGE = "ch.dissem.abit.DeleteMessage"
        const val EXTRA_STARTUP_NODE = "ch.dissem.abit.StartFullNode"
        const val EXTRA_SHUTDOWN_NODE = "ch.dissem.abit.StopFullNode"
    }
}
