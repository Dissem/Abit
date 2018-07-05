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
import ch.dissem.apps.abit.util.Constants.PREFERENCE_EMULATE_CONVERSATIONS
import ch.dissem.apps.abit.util.Constants.PREFERENCE_ONLINE
import ch.dissem.apps.abit.util.Constants.PREFERENCE_REQUEST_ACK
import ch.dissem.apps.abit.util.Constants.PREFERENCE_REQUIRE_CHARGING
import ch.dissem.apps.abit.util.Constants.PREFERENCE_WIFI_ONLY
import org.jetbrains.anko.batteryManager
import org.jetbrains.anko.connectivityManager
import org.jetbrains.anko.defaultSharedPreferences
import org.slf4j.LoggerFactory
import java.io.File


/**
 * @author Christian Basler
 */
object Preferences {
    private val LOG = LoggerFactory.getLogger(Preferences::class.java)

    fun isConnectionAllowed(ctx: Context) = isAllowedForWiFi(ctx) && isAllowedForCharging(ctx)

    private fun isAllowedForWiFi(ctx: Context) = !isWifiOnly(ctx) || !ctx.connectivityManager.isActiveNetworkMetered

    private fun isAllowedForCharging(ctx: Context) = !requireCharging(ctx) || isCharging(ctx)

    private fun isCharging(ctx: Context) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        ctx.batteryManager.isCharging
    } else {
        val intent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    fun isWifiOnly(ctx: Context) = ctx.defaultSharedPreferences.getBoolean(PREFERENCE_WIFI_ONLY, true)

    fun setWifiOnly(ctx: Context, status: Boolean) {
        ctx.defaultSharedPreferences.edit()
            .putBoolean(PREFERENCE_WIFI_ONLY, status)
            .apply()
    }

    fun requireCharging(ctx: Context) = ctx.defaultSharedPreferences.getBoolean(PREFERENCE_REQUIRE_CHARGING, true)

    fun isEmulateConversations(ctx: Context) =
        ctx.defaultSharedPreferences.getBoolean(PREFERENCE_EMULATE_CONVERSATIONS, true)

    fun getExportDirectory(ctx: Context) = File(ctx.filesDir, "exports")

    fun requestAcknowledgements(ctx: Context) = ctx.defaultSharedPreferences.getBoolean(PREFERENCE_REQUEST_ACK, true)

    fun cleanupExportDirectory(ctx: Context) {
        val exportDirectory = getExportDirectory(ctx)
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

    fun isOnline(ctx: Context) = ctx.defaultSharedPreferences.getBoolean(PREFERENCE_ONLINE, true)

    fun setOnline(ctx: Context, status: Boolean) {
        ctx.defaultSharedPreferences.edit()
            .putBoolean(PREFERENCE_ONLINE, status)
            .apply()
        if (status) {
            NetworkUtils.enableNode(ctx, true)
        } else {
            NetworkUtils.disableNode(ctx)
        }
    }

}
