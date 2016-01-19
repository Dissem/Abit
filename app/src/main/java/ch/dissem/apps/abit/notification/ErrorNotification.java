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

import android.content.Context;
import android.support.annotation.StringRes;
import android.support.v7.app.NotificationCompat;

import ch.dissem.apps.abit.R;

/**
 * Easily create notifications with error messages. Use carefully, users probably won't like them.
 * (But they are useful during development/testing)
 *
 * @author Christian Basler
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

    public ErrorNotification setWarning(@StringRes int resId, Object... args) {
        builder.setSmallIcon(R.drawable.ic_notification_warning)
                .setContentText(ctx.getString(resId, args));
        notification = builder.build();
        return this;
    }

    public ErrorNotification setError(@StringRes int resId, Object... args) {
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
