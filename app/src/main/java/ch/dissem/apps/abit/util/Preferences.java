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

package ch.dissem.apps.abit.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;

import ch.dissem.apps.abit.R;
import ch.dissem.apps.abit.listener.WifiReceiver;
import ch.dissem.apps.abit.notification.ErrorNotification;

import static ch.dissem.apps.abit.util.Constants.PREFERENCE_SYNC_TIMEOUT;
import static ch.dissem.apps.abit.util.Constants.PREFERENCE_TRUSTED_NODE;
import static ch.dissem.apps.abit.util.Constants.PREFERENCE_WIFI_ONLY;

/**
 * @author Christian Basler
 */
public class Preferences {
    public static boolean useTrustedNode(Context ctx) {
        String trustedNode = getPreference(ctx, PREFERENCE_TRUSTED_NODE);
        return trustedNode != null && !trustedNode.trim().isEmpty();
    }

    /**
     * Warning, this method might do a network call and therefore can't be called from
     * the UI thread.
     */
    public static InetAddress getTrustedNode(Context ctx) throws IOException {
        String trustedNode = getPreference(ctx, PREFERENCE_TRUSTED_NODE);
        if (trustedNode == null) return null;
        trustedNode = trustedNode.trim();
        if (trustedNode.isEmpty()) return null;

        if (trustedNode.matches("^(?![0-9a-fA-F]*:[0-9a-fA-F]*:).*(:[0-9]+)$")) {
            int index = trustedNode.lastIndexOf(':');
            trustedNode = trustedNode.substring(0, index);
        }
            return InetAddress.getByName(trustedNode);
    }

    public static int getTrustedNodePort(Context ctx) {
        String trustedNode = getPreference(ctx, PREFERENCE_TRUSTED_NODE);
        if (trustedNode == null) return 8444;
        trustedNode = trustedNode.trim();

        if (trustedNode.matches("^(?![0-9a-fA-F]*:[0-9a-fA-F]*:).*(:[0-9]+)$")) {
            int index = trustedNode.lastIndexOf(':');
            String portString = trustedNode.substring(index + 1);
            try {
                return Integer.parseInt(portString);
            } catch (NumberFormatException e) {
                new ErrorNotification(ctx)
                        .setError(R.string.error_invalid_sync_port, portString)
                        .show();
            }
        }
        return 8444;
    }

    public static long getTimeoutInSeconds(Context ctx) {
        String preference = getPreference(ctx, PREFERENCE_SYNC_TIMEOUT);
        return preference == null ? 120 : Long.parseLong(preference);
    }

    private static String getPreference(Context ctx, String name) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);

        return preferences.getString(name, null);
    }

    public static boolean isConnectionAllowed(Context ctx) {
        return !isWifiOnly(ctx) || !WifiReceiver.isConnectedToMeteredNetwork(ctx);
    }

    public static boolean isWifiOnly(Context ctx) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
        return preferences.getBoolean(PREFERENCE_WIFI_ONLY, true);
    }
}
