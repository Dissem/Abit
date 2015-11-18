package ch.dissem.apps.abit.util;

import java.util.regex.Pattern;

/**
 * Created by chrigu on 16.11.15.
 */
public class Constants {
    public static final String BITMESSAGE_URL_SCHEMA = "bitmessage:";
    public static final Pattern BITMESSAGE_ADDRESS_PATTERN = Pattern.compile("\\bBM-[a-zA-Z0-9]+\\b");
}
