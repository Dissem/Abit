/*
 * Copyright 2015 Christian Basler
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

import android.content.Context;

import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ch.dissem.apps.abit.MainActivity;
import ch.dissem.apps.abit.notification.NewMessageNotification;
import ch.dissem.bitmessage.BitmessageContext;
import ch.dissem.bitmessage.entity.Plaintext;

/**
 * Listens for decrypted Bitmessage messages. Does show a notification.
 * <p>
 * Should show a notification when the app isn't running, but update the message list when it is.
 * Also,
 * notifications should be combined.
 * </p>
 */
public class MessageListener implements BitmessageContext.Listener {
    private final Deque<Plaintext> unacknowledged = new LinkedList<>();
    private int numberOfUnacknowledgedMessages = 0;
    private final NewMessageNotification notification;
    private final ExecutorService pool = Executors.newSingleThreadExecutor();

    public MessageListener(Context ctx) {
        this.notification = new NewMessageNotification(ctx);
    }

    @Override
    public void receive(final Plaintext plaintext) {
        pool.submit(() -> {
            unacknowledged.addFirst(plaintext);
            numberOfUnacknowledgedMessages++;
            if (unacknowledged.size() > 5) {
                unacknowledged.removeLast();
            }
            if (numberOfUnacknowledgedMessages == 1) {
                notification.singleNotification(plaintext);
            } else {
                notification.multiNotification(unacknowledged, numberOfUnacknowledgedMessages);
            }
            notification.show();

            // If MainActivity is shown, update the sidebar badges
            MainActivity main = MainActivity.getInstance();
            if (main != null) {
                main.updateUnread();
            }
        });
    }

    public void resetNotification() {
        pool.submit(() -> {
            notification.hide();
            unacknowledged.clear();
            numberOfUnacknowledgedMessages = 0;
        });
    }
}
