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

package ch.dissem.apps.abit.service;

import android.app.IntentService;
import android.content.Intent;

import ch.dissem.apps.abit.dialog.FullNodeDialogActivity;
import ch.dissem.apps.abit.util.Preferences;
import ch.dissem.bitmessage.BitmessageContext;
import ch.dissem.bitmessage.entity.Plaintext;

import static ch.dissem.apps.abit.MainActivity.updateNodeSwitch;

/**
 * @author Christian Basler
 */

public class BitmessageIntentService extends IntentService {
    public static final String EXTRA_DELETE_MESSAGE = "ch.dissem.abit.DeleteMessage";
    public static final String EXTRA_STARTUP_NODE = "ch.dissem.abit.StartFullNode";
    public static final String EXTRA_SHUTDOWN_NODE = "ch.dissem.abit.StopFullNode";

    private BitmessageContext bmc;

    public BitmessageIntentService() {
        super("BitmessageIntentService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        bmc = Singleton.getBitmessageContext(this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent.hasExtra(EXTRA_DELETE_MESSAGE)) {
            Plaintext item = (Plaintext) intent.getSerializableExtra(EXTRA_DELETE_MESSAGE);
            bmc.labeler().delete(item);
            bmc.messages().save(item);
            Singleton.getMessageListener(this).resetNotification();
        }
        if (intent.hasExtra(EXTRA_STARTUP_NODE)) {
            if (Preferences.isConnectionAllowed(this)) {
                startService(new Intent(this, BitmessageService.class));
                updateNodeSwitch();
            } else {
                Intent dialogIntent = new Intent(this, FullNodeDialogActivity.class);
                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(dialogIntent);
                sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
            }
        }
        if (intent.hasExtra(EXTRA_SHUTDOWN_NODE)) {
            stopService(new Intent(this, BitmessageService.class));
        }
    }
}
