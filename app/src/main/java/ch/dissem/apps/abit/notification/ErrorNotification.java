package ch.dissem.apps.abit.notification;

import android.content.Context;
import android.support.v7.app.NotificationCompat;

import ch.dissem.apps.abit.R;

/**
 * Created by chrigu on 29.10.15.
 */
public class ErrorNotification extends AbstractNotification {
    public static final int ERROR_NOTIFICATION_ID = 4;

    private NotificationCompat.Builder builder;

    public ErrorNotification(Context ctx) {
        super(ctx);
        builder = new NotificationCompat.Builder(ctx);
        builder.setContentTitle(ctx.getString(R.string.app_name))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
    }

    public ErrorNotification setWarning(int resId, Object... args) {
        builder.setSmallIcon(R.drawable.ic_notification_warning)
                .setContentText(ctx.getString(resId, args));
        notification = builder.build();
        return this;
    }

    public ErrorNotification setError(int resId, Object... args) {
        builder.setSmallIcon(R.drawable.ic_notification_error)
                .setContentText(ctx.getString(resId, args));
        notification = builder.build();
        return this;
    }

    @Override
    protected int getNotificationId() {
        return ERROR_NOTIFICATION_ID;
    }
}
