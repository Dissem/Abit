package ch.dissem.apps.abit.util;

import java.util.regex.Pattern;

/**
 * @author Christian Basler
 */
public class Constants {
    public static final String PREFERENCE_WIFI_ONLY = "wifi_only";
    public static final String PREFERENCE_TRUSTED_NODE = "trusted_node";
    public static final String PREFERENCE_SYNC_TIMEOUT = "sync_timeout";
    public static final String PREFERENCE_SERVER_POW = "server_pow";

    public static final String BITMESSAGE_URL_SCHEMA = "bitmessage:";
    public static final Pattern BITMESSAGE_ADDRESS_PATTERN = Pattern.compile("\\bBM-[a-zA-Z0-9]+\\b");
}
