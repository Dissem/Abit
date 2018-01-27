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

package ch.dissem.apps.abit.listener

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ch.dissem.apps.abit.service.Singleton
import ch.dissem.apps.abit.util.NetworkUtils
import ch.dissem.apps.abit.util.Preferences
import org.jetbrains.anko.connectivityManager

class WifiReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if ("android.net.conn.CONNECTIVITY_CHANGE" == intent.action) {
            val bmc = Singleton.getBitmessageContext(ctx)
            if (Preferences.isFullNodeActive(ctx) && !bmc.isRunning() && !(Preferences.isWifiOnly(ctx) && ctx.connectivityManager.isActiveNetworkMetered)) {
                NetworkUtils.doStartBitmessageService(ctx)
            }
        }
    }
}
