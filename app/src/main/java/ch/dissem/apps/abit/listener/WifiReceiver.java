package ch.dissem.apps.abit.listener;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import ch.dissem.apps.abit.service.Singleton;
import ch.dissem.apps.abit.util.Preferences;
import ch.dissem.bitmessage.BitmessageContext;

public class WifiReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (Preferences.isWifiOnly(ctx)) {
            BitmessageContext bmc = Singleton.getBitmessageContext(ctx);

            if (!isConnectedToWifi(ctx) && bmc.isRunning()) {
                bmc.shutdown();
            }
        }
    }

    public static boolean isConnectedToWifi(Context ctx) {
        NetworkInfo netInfo = getNetworkInfo(ctx);
        return netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI;
    }

    private static NetworkInfo getNetworkInfo(Context ctx) {
        ConnectivityManager conMan = (ConnectivityManager) ctx.getSystemService(Context
                .CONNECTIVITY_SERVICE);
        return conMan.getActiveNetworkInfo();
    }
}