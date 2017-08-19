package ch.dissem.apps.abit.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ch.dissem.apps.abit.util.NetworkUtils
import ch.dissem.apps.abit.util.Preferences

/**
 * Created by chrigu on 18.08.17.
 */
class StartServiceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (Preferences.isFullNodeActive(context)) {
            NetworkUtils.enableNode(context, false)
        }
    }
}
