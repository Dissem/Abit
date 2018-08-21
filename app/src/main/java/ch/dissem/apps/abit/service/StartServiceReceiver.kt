package ch.dissem.apps.abit.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_BOOT_COMPLETED
import ch.dissem.apps.abit.util.network
import ch.dissem.apps.abit.util.preferences

/**
 * Starts the Bitmessage "full node" service if conditions allow it
 */
class StartServiceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == ACTION_BOOT_COMPLETED && context.preferences.online) {
            context.network.enableNode(false)
        }
    }
}
