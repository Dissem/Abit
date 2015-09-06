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

package ch.dissem.apps.abit.listeners;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.v7.app.NotificationCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import ch.dissem.apps.abit.Identicon;
import ch.dissem.apps.abit.MessageListActivity;
import ch.dissem.apps.abit.R;
import ch.dissem.bitmessage.BitmessageContext;
import ch.dissem.bitmessage.entity.Plaintext;

import java.util.LinkedList;

/**
 * Listens for decrypted Bitmessage messages. Does show a notification.
 * <p>
 * Should show a notification when the app isn't running, but update the message list when it is. Also,
 * notifications should be combined.
 * </p>
 */
public class MessageListener implements BitmessageContext.Listener {
    private static final StyleSpan SPAN_EMPHASIS = new StyleSpan(Typeface.BOLD);
    private final Context ctx;
    private final NotificationManager manager;
    private final LinkedList<Plaintext> unacknowledged = new LinkedList<>();
    private final int pictureSize;
    private int numberOfUnacknowledgedMessages = 0;

    public MessageListener(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        this.manager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);

        this.pictureSize = getMaxContactPhotoSize(ctx);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public static int getMaxContactPhotoSize(final Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            // Note that this URI is safe to call on the UI thread.
            final Uri uri = ContactsContract.DisplayPhoto.CONTENT_MAX_DIMENSIONS_URI;
            final String[] projection = new String[]{ContactsContract.DisplayPhoto.DISPLAY_MAX_DIM};
            final Cursor c = context.getContentResolver().query(uri, projection, null, null, null);
            try {
                c.moveToFirst();
                return c.getInt(0);
            } finally {
                c.close();
            }
        }
        // fallback: 96x96 is the max contact photo size for pre-ICS versions
        return 96;
    }

    @Override
    public void receive(final Plaintext plaintext) {
        synchronized (unacknowledged) {
            unacknowledged.addFirst(plaintext);
            numberOfUnacknowledgedMessages++;
            if (unacknowledged.size() > 5) {
                unacknowledged.removeLast();
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx);
        if (numberOfUnacknowledgedMessages == 1) {
            Spannable bigText = new SpannableString(plaintext.getSubject() + "\n" + plaintext.getText());
            bigText.setSpan(SPAN_EMPHASIS, 0, plaintext.getSubject().length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            builder.setSmallIcon(R.drawable.ic_notification_new_message)
                    .setLargeIcon(toBitmap(new Identicon(plaintext.getFrom())))
                    .setContentTitle(plaintext.getFrom().toString())
                    .setContentText(plaintext.getSubject())
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText))
                    .setContentInfo("Info");

            Intent showMessageIntent = new Intent(ctx, MessageListActivity.class);
            showMessageIntent.putExtra(MessageListActivity.EXTRA_SHOW_MESSAGE, plaintext);
            PendingIntent pendingIntent = PendingIntent.getActivity(ctx, 0, showMessageIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(pendingIntent);

            builder.addAction(R.drawable.ic_action_reply, ctx.getString(R.string.reply), pendingIntent);
            builder.addAction(R.drawable.ic_action_delete, ctx.getString(R.string.delete), pendingIntent);
        } else {
            builder.setSmallIcon(R.drawable.ic_notification_new_message)
                    .setContentTitle(ctx.getString(R.string.n_new_messages, this.unacknowledged.size()))
                    .setContentText(ctx.getString(R.string.app_name));

            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
            synchronized (unacknowledged) {
                inboxStyle.setBigContentTitle(ctx.getString(R.string.n_new_messages, numberOfUnacknowledgedMessages));
                for (Plaintext msg : unacknowledged) {
                    Spannable sb = new SpannableString(msg.getFrom() + " " + msg.getSubject());
                    sb.setSpan(SPAN_EMPHASIS, 0, String.valueOf(msg.getFrom()).length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                    inboxStyle.addLine(sb);
                }
            }
            builder.setStyle(inboxStyle);

            Intent intent = new Intent(ctx, MessageListActivity.class);
            intent.setAction(MessageListActivity.ACTION_SHOW_INBOX);
            PendingIntent pendingIntent = PendingIntent.getActivity(ctx, 1, intent, 0);
            builder.setContentIntent(pendingIntent);
        }

        manager.notify(0, builder.build());
    }

    private Bitmap toBitmap(Identicon identicon) {
        Bitmap bitmap = Bitmap.createBitmap(pictureSize, pictureSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        identicon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        identicon.draw(canvas);
        return bitmap;
    }

    public void resetNotification() {
        manager.cancel(0);
        synchronized (unacknowledged) {
            unacknowledged.clear();
            numberOfUnacknowledgedMessages = 0;
        }
    }
}
