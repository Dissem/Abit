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

package ch.dissem.apps.abit.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import ch.dissem.apps.abit.service.Singleton
import ch.dissem.apps.abit.util.Constants.PREFERENCE_EMULATE_CONVERSATIONS
import ch.dissem.apps.abit.util.Constants.PREFERENCE_ONLINE
import ch.dissem.apps.abit.util.Constants.PREFERENCE_REQUEST_ACK
import ch.dissem.apps.abit.util.Constants.PREFERENCE_REQUIRE_CHARGING
import ch.dissem.apps.abit.util.Constants.PREFERENCE_SEPARATE_IDENTITIES
import ch.dissem.apps.abit.util.Constants.PREFERENCE_WIFI_ONLY
import org.jetbrains.anko.batteryManager
import org.jetbrains.anko.connectivityManager
import org.jetbrains.anko.defaultSharedPreferences
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.ref.WeakReference


val Context.preferences get() = Preferences.getInstance(this)

/**
 * @author Christian Basler
 */
class Preferences internal constructor(private val ctx: Context) {
    private val LOG = LoggerFactory.getLogger(Preferences::class.java)

    val connectionAllowed get() = isAllowedForWiFi && isAllowedForCharging

    private val isAllowedForWiFi get() = !wifiOnly || !ctx.connectivityManager.isActiveNetworkMetered

    private val isAllowedForCharging get() = !requireCharging || isCharging

    private val sharedPreferences = ctx.defaultSharedPreferences

    private val isCharging
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ctx.batteryManager.isCharging
        } else {
            val intent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        }

    var wifiOnly
        get() = sharedPreferences.getBoolean(PREFERENCE_WIFI_ONLY, true)
        set(value) {
            sharedPreferences.edit()
                .putBoolean(PREFERENCE_WIFI_ONLY, value)
                .apply()
        }

    val requireCharging get() = sharedPreferences.getBoolean(PREFERENCE_REQUIRE_CHARGING, true)

    val emulateConversations get() = sharedPreferences.getBoolean(PREFERENCE_EMULATE_CONVERSATIONS, true)

    val exportDirectory by lazy { File(ctx.filesDir, "exports") }

    val requestAcknowledgements = sharedPreferences.getBoolean(PREFERENCE_REQUEST_ACK, true)

    fun cleanupExportDirectory() {
        if (exportDirectory.exists()) {
            exportDirectory.listFiles().forEach { file ->
                try {
                    if (!file.delete()) {
                        file.deleteOnExit()
                    }
                } catch (e: Exception) {
                    LOG.debug(e.message, e)
                }
            }
        }
    }

    var online
        get() = sharedPreferences.getBoolean(PREFERENCE_ONLINE, true)
        set(value) {
            sharedPreferences.edit()
                .putBoolean(PREFERENCE_ONLINE, value)
                .apply()
            if (value) {
                ctx.network.enableNode(true)
            } else {
                ctx.network.disableNode()
            }
        }

    val separateIdentities
        get() = sharedPreferences.getBoolean(PREFERENCE_SEPARATE_IDENTITIES, false)

    val currentIdentity
        get() = Singleton.getIdentity(ctx)

    val listeningPort
        get() = sharedPreferences.getString("listening_port", null)?.toIntOrNull()
            ?: 8444

    companion object {
        private var instance: WeakReference<Preferences>? = null

        internal fun getInstance(ctx: Context): Preferences {
            var prefs = instance?.get()
            if (prefs == null) {
                prefs = Preferences(ctx.applicationContext)
                instance = WeakReference(prefs)
            }
            return prefs
        }
    }
}
