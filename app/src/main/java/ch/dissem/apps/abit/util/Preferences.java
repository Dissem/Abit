package ch.dissem.apps.abit.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

import ch.dissem.apps.abit.R;
import ch.dissem.apps.abit.notification.ErrorNotification;

import static ch.dissem.apps.abit.util.Constants.PREFERENCE_SERVER_POW;
import static ch.dissem.apps.abit.util.Constants.PREFERENCE_SYNC_TIMEOUT;
import static ch.dissem.apps.abit.util.Constants.PREFERENCE_TRUSTED_NODE;

/**
 * Created by chrig on 01.12.2015.
 */
public class Preferences {
    private static Logger LOG = LoggerFactory.getLogger(Preferences.class);

    public static boolean useTrustedNode(Context ctx) {
        String trustedNode = getPreference(ctx, PREFERENCE_TRUSTED_NODE);
        return trustedNode == null || trustedNode.trim().isEmpty();
    }

    /**
     * Warning, this method might do a network call and therefore can't be called from
     * the UI thread.
     */
    public static InetAddress getTrustedNode(Context ctx) {
        String trustedNode = getPreference(ctx, PREFERENCE_TRUSTED_NODE);
        if (trustedNode == null) return null;
        trustedNode = trustedNode.trim();
        if (trustedNode.isEmpty()) return null;

        if (trustedNode.matches("^(?![0-9a-fA-F]*:[0-9a-fA-F]*:).*(:[0-9]+)$")) {
            int index = trustedNode.lastIndexOf(':');
            trustedNode = trustedNode.substring(0, index);
        }
        try {
            return InetAddress.getByName(trustedNode);
        } catch (UnknownHostException e) {
            new ErrorNotification(ctx)
                    .setError(R.string.error_invalid_sync_host)
                    .show();
            LOG.error(e.getMessage(), e);
            return null;
        }
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

    public static boolean isServerPOW(Context ctx) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
        return preferences.getBoolean(PREFERENCE_SERVER_POW, false);
    }

    private static String getPreference(Context ctx, String name) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);

        return preferences.getString(name, null);
    }
}
