package ch.dissem.apps.abit.notification;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.NotificationCompat;

import ch.dissem.apps.abit.MessageListActivity;
import ch.dissem.apps.abit.R;

/**
 * Ongoing notification while proof of work is in progress.
 */
public class ProofOfWorkNotification extends AbstractNotification {
    public static final int ONGOING_NOTIFICATION_ID = 3;

    public ProofOfWorkNotification(Context ctx) {
        super(ctx);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx);

        Intent showMessageIntent = new Intent(ctx, MessageListActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(ctx, 0, showMessageIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setUsesChronometer(true)
                .setSmallIcon(R.drawable.ic_notification_proof_of_work)
                .setContentTitle(ctx.getString(R.string.proof_of_work_title))
                .setContentText(ctx.getString(R.string.proof_of_work_text))
                .setContentIntent(pendingIntent);

        notification = builder.build();
    }

    @Override
    protected int getNotificationId() {
        return ONGOING_NOTIFICATION_ID;
    }
}
