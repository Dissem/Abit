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
import android.preference.PreferenceManager
import ch.dissem.apps.abit.R
import ch.dissem.apps.abit.notification.ErrorNotification
import ch.dissem.apps.abit.util.Constants.PREFERENCE_FULL_NODE
import ch.dissem.apps.abit.util.Constants.PREFERENCE_REQUEST_ACK
import ch.dissem.apps.abit.util.Constants.PREFERENCE_SYNC_TIMEOUT
import ch.dissem.apps.abit.util.Constants.PREFERENCE_TRUSTED_NODE
import ch.dissem.apps.abit.util.Constants.PREFERENCE_WIFI_ONLY
import org.jetbrains.anko.connectivityManager
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.net.InetAddress

/**
 * @author Christian Basler
 */
object Preferences {
    private val LOG = LoggerFactory.getLogger(Preferences::class.java)

    fun useTrustedNode(ctx: Context): Boolean {
        val trustedNode = getPreference(ctx, PREFERENCE_TRUSTED_NODE) ?: return false
        return trustedNode.trim { it <= ' ' }.isNotEmpty()
    }

    /**
     * Warning, this method might do a network call and therefore can't be called from
     * the UI thread.
     */
    @Throws(IOException::class)
    fun getTrustedNode(ctx: Context): InetAddress? {
        var trustedNode: String = getPreference(ctx, PREFERENCE_TRUSTED_NODE) ?: return null
        trustedNode = trustedNode.trim { it <= ' ' }
        if (trustedNode.isEmpty()) return null

        if (trustedNode.matches("^(?![0-9a-fA-F]*:[0-9a-fA-F]*:).*(:[0-9]+)$".toRegex())) {
            val index = trustedNode.lastIndexOf(':')
            trustedNode = trustedNode.substring(0, index)
        }
        return InetAddress.getByName(trustedNode)
    }

    fun getTrustedNodePort(ctx: Context): Int {
        var trustedNode: String = getPreference(ctx, PREFERENCE_TRUSTED_NODE) ?: return 8444
        trustedNode = trustedNode.trim { it <= ' ' }

        if (trustedNode.matches("^(?![0-9a-fA-F]*:[0-9a-fA-F]*:).*(:[0-9]+)$".toRegex())) {
            val index = trustedNode.lastIndexOf(':')
            val portString = trustedNode.substring(index + 1)
            try {
                return Integer.parseInt(portString)
            } catch (e: NumberFormatException) {
                ErrorNotification(ctx)
                        .setError(R.string.error_invalid_sync_port, portString)
                        .show()
            }
        }
        return 8444
    }

    fun getTimeoutInSeconds(ctx: Context): Long {
        val preference = getPreference(ctx, PREFERENCE_SYNC_TIMEOUT) ?: return 120
        return preference.toLong()
    }

    private fun getPreference(ctx: Context, name: String): String? {
        val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)

        return preferences.getString(name, null)
    }

    fun isConnectionAllowed(ctx: Context) = !isWifiOnly(ctx) || !ctx.connectivityManager.isActiveNetworkMetered

    fun isWifiOnly(ctx: Context): Boolean {
        val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)
        return preferences.getBoolean(PREFERENCE_WIFI_ONLY, true)
    }

    fun setWifiOnly(ctx: Context, status: Boolean) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)
        preferences.edit().putBoolean(PREFERENCE_WIFI_ONLY, status).apply()
    }

    fun isFullNodeActive(ctx: Context): Boolean {
        val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)
        return preferences.getBoolean(PREFERENCE_FULL_NODE, false)
    }

    fun setFullNodeActive(ctx: Context, status: Boolean) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)
        preferences.edit().putBoolean(PREFERENCE_FULL_NODE, status).apply()
    }

    fun getExportDirectory(ctx: Context) = File(ctx.filesDir, "exports")

    fun requestAcknowledgements(ctx: Context): Boolean {
        val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)
        return preferences.getBoolean(PREFERENCE_REQUEST_ACK, true)
    }

    fun setRequestAcknowledgements(ctx: Context, status: Boolean) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)
        preferences.edit().putBoolean(PREFERENCE_REQUEST_ACK, status).apply()
    }

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
}
