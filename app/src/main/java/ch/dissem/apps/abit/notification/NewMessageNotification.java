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

package ch.dissem.apps.abit.notification;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.support.v7.app.NotificationCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;

import java.util.Collection;

import ch.dissem.apps.abit.Identicon;
import ch.dissem.apps.abit.MainActivity;
import ch.dissem.apps.abit.R;
import ch.dissem.apps.abit.service.BitmessageIntentService;
import ch.dissem.bitmessage.entity.Plaintext;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static ch.dissem.apps.abit.MainActivity.EXTRA_REPLY_TO_MESSAGE;
import static ch.dissem.apps.abit.MainActivity.EXTRA_SHOW_MESSAGE;
import static ch.dissem.apps.abit.service.BitmessageIntentService.EXTRA_DELETE_MESSAGE;
import static ch.dissem.apps.abit.util.Drawables.toBitmap;

public class NewMessageNotification extends AbstractNotification {
    private static final int NEW_MESSAGE_NOTIFICATION_ID = 1;
    private static final StyleSpan SPAN_EMPHASIS = new StyleSpan(Typeface.BOLD);

    public NewMessageNotification(Context ctx) {
        super(ctx);
    }

    public NewMessageNotification singleNotification(Plaintext plaintext) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx);
        Spannable bigText = new SpannableString(plaintext.getSubject() + "\n" + plaintext.getText
            ());
        bigText.setSpan(SPAN_EMPHASIS, 0, plaintext.getSubject().length(), Spanned
            .SPAN_INCLUSIVE_EXCLUSIVE);
        builder.setSmallIcon(R.drawable.ic_notification_new_message)
            .setLargeIcon(toBitmap(new Identicon(plaintext.getFrom()), 192))
            .setContentTitle(plaintext.getFrom().toString())
            .setContentText(plaintext.getSubject())
            .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText))
            .setContentInfo("Info");

        builder.setContentIntent(
            createActivityIntent(EXTRA_SHOW_MESSAGE, plaintext));
        builder.addAction(R.drawable.ic_action_reply, ctx.getString(R.string.reply),
            createActivityIntent(EXTRA_REPLY_TO_MESSAGE, plaintext));
        builder.addAction(R.drawable.ic_action_delete, ctx.getString(R.string.delete),
            createServiceIntent(ctx, EXTRA_DELETE_MESSAGE, plaintext));
        notification = builder.build();
        return this;
    }

    private PendingIntent createActivityIntent(String action, Plaintext message) {
        Intent intent = new Intent(ctx, MainActivity.class);
        intent.putExtra(action, message);
        return PendingIntent.getActivity(ctx, action.hashCode(), intent, FLAG_UPDATE_CURRENT);
    }

    private PendingIntent createServiceIntent(Context ctx, String action, Plaintext message) {
        Intent intent = new Intent(ctx, BitmessageIntentService.class);
        intent.putExtra(action, message);
        return PendingIntent.getService(ctx, action.hashCode(), intent, FLAG_UPDATE_CURRENT);
    }

    /**
     * @param unacknowledged will be accessed from different threads, so make sure wherever it's
     *                       accessed it will be in a <code>synchronized(unacknowledged)
     *                       {}</code> block
     */
    public NewMessageNotification multiNotification(Collection<Plaintext> unacknowledged, int
        numberOfUnacknowledgedMessages) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx);
        builder.setSmallIcon(R.drawable.ic_notification_new_message)
            .setContentTitle(ctx.getString(R.string.n_new_messages, unacknowledged.size()))
            .setContentText(ctx.getString(R.string.app_name));

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (unacknowledged) {
            inboxStyle.setBigContentTitle(ctx.getString(R.string.n_new_messages,
                numberOfUnacknowledgedMessages));
            for (Plaintext msg : unacknowledged) {
                Spannable sb = new SpannableString(msg.getFrom() + " " + msg.getSubject());
                sb.setSpan(SPAN_EMPHASIS, 0, String.valueOf(msg.getFrom()).length(), Spannable
                    .SPAN_INCLUSIVE_EXCLUSIVE);
                inboxStyle.addLine(sb);
            }
        }
        builder.setStyle(inboxStyle);

        Intent intent = new Intent(ctx, MainActivity.class);
        intent.setAction(MainActivity.ACTION_SHOW_INBOX);
        PendingIntent pendingIntent = PendingIntent.getActivity(ctx, 1, intent, 0);
        builder.setContentIntent(pendingIntent);
        notification = builder.build();
        return this;
    }

    @Override
    protected int getNotificationId() {
        return NEW_MESSAGE_NOTIFICATION_ID;
    }
}
