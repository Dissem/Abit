package ch.dissem.apps.abit.listener;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.dissem.apps.abit.service.Singleton;
import ch.dissem.apps.abit.util.Preferences;
import ch.dissem.bitmessage.BitmessageContext;

public class WifiReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (Preferences.isWifiOnly(ctx)) {
            BitmessageContext bmc = Singleton.getBitmessageContext(ctx);
            ConnectivityManager conMan = (ConnectivityManager) ctx.getSystemService(Context
                    .CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = conMan.getActiveNetworkInfo();

            if (netInfo != null && netInfo.getType() != ConnectivityManager.TYPE_WIFI
                    && !bmc.isRunning()) {
                bmc.shutdown();
            }
        }
    }

    public static boolean isConnectedToWifi(Context ctx) {
        WifiManager wifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        SupplicantState state = wifiManager.getConnectionInfo().getSupplicantState();
        return state == SupplicantState.COMPLETED;
    }
}