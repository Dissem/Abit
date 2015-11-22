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

import java.util.LinkedList;

import ch.dissem.apps.abit.Identicon;
import ch.dissem.apps.abit.MainActivity;
import ch.dissem.apps.abit.R;
import ch.dissem.bitmessage.entity.Plaintext;

import static ch.dissem.apps.abit.util.Drawables.toBitmap;

public class NewMessageNotification extends AbstractNotification {
    public static final int NEW_MESSAGE_NOTIFICATION_ID = 1;
    private static final StyleSpan SPAN_EMPHASIS = new StyleSpan(Typeface.BOLD);

    public NewMessageNotification(Context ctx) {
        super(ctx);
    }

    public NewMessageNotification singleNotification(Plaintext plaintext) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx);
        Spannable bigText = new SpannableString(plaintext.getSubject() + "\n" + plaintext.getText());
        bigText.setSpan(SPAN_EMPHASIS, 0, plaintext.getSubject().length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        builder.setSmallIcon(R.drawable.ic_notification_new_message)
                .setLargeIcon(toBitmap(new Identicon(plaintext.getFrom()), 192))
                .setContentTitle(plaintext.getFrom().toString())
                .setContentText(plaintext.getSubject())
                .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText))
                .setContentInfo("Info");

        Intent showMessageIntent = new Intent(ctx, MainActivity.class);
        showMessageIntent.putExtra(MainActivity.EXTRA_SHOW_MESSAGE, plaintext);
        PendingIntent pendingIntent = PendingIntent.getActivity(ctx, 0, showMessageIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);

        builder.addAction(R.drawable.ic_action_reply, ctx.getString(R.string.reply), pendingIntent);
        builder.addAction(R.drawable.ic_action_delete, ctx.getString(R.string.delete), pendingIntent);
        notification = builder.build();
        return this;
    }

    public NewMessageNotification multiNotification(LinkedList<Plaintext> unacknowledged, int numberOfUnacknowledgedMessages) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx);
        builder.setSmallIcon(R.drawable.ic_notification_new_message)
                .setContentTitle(ctx.getString(R.string.n_new_messages, unacknowledged.size()))
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
