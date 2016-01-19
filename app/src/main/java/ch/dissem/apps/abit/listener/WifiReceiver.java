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