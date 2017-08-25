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

package ch.dissem.apps.abit.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.support.v7.app.NotificationCompat
import android.support.v4.app.NotificationCompat.BigTextStyle
import android.support.v4.app.NotificationCompat.InboxStyle
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan

import ch.dissem.apps.abit.Identicon
import ch.dissem.apps.abit.MainActivity
import ch.dissem.apps.abit.R
import ch.dissem.apps.abit.service.BitmessageIntentService
import ch.dissem.bitmessage.entity.Plaintext

import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import ch.dissem.apps.abit.MainActivity.Companion.EXTRA_REPLY_TO_MESSAGE
import ch.dissem.apps.abit.MainActivity.Companion.EXTRA_SHOW_MESSAGE
import ch.dissem.apps.abit.service.BitmessageIntentService.Companion.EXTRA_DELETE_MESSAGE
import ch.dissem.apps.abit.util.Drawables.toBitmap

class NewMessageNotification(ctx: Context) : AbstractNotification(ctx) {

    fun singleNotification(plaintext: Plaintext): NewMessageNotification {
        val builder = NotificationCompat.Builder(ctx)
        val bigText = SpannableString(plaintext.subject + "\n" + plaintext.text)
        bigText.setSpan(SPAN_EMPHASIS, 0, plaintext.subject!!.length, Spanned
                .SPAN_INCLUSIVE_EXCLUSIVE)
        builder.setSmallIcon(R.drawable.ic_notification_new_message)
                .setLargeIcon(toBitmap(Identicon(plaintext.from), 192))
                .setContentTitle(plaintext.from.toString())
                .setContentText(plaintext.subject)
                .setStyle(BigTextStyle().bigText(bigText))
                .setContentInfo("Info")

        builder.setContentIntent(
                createActivityIntent(EXTRA_SHOW_MESSAGE, plaintext))
        builder.addAction(R.drawable.ic_action_reply, ctx.getString(R.string.reply),
                createActivityIntent(EXTRA_REPLY_TO_MESSAGE, plaintext))
        builder.addAction(R.drawable.ic_action_delete, ctx.getString(R.string.delete),
                createServiceIntent(ctx, EXTRA_DELETE_MESSAGE, plaintext))
        notification = builder.build()
        return this
    }

    private fun createActivityIntent(action: String, message: Plaintext): PendingIntent {
        val intent = Intent(ctx, MainActivity::class.java)
        intent.putExtra(action, message)
        return PendingIntent.getActivity(ctx, action.hashCode(), intent, FLAG_UPDATE_CURRENT)
    }

    private fun createServiceIntent(ctx: Context, action: String, message: Plaintext): PendingIntent {
        val intent = Intent(ctx, BitmessageIntentService::class.java)
        intent.putExtra(action, message)
        return PendingIntent.getService(ctx, action.hashCode(), intent, FLAG_UPDATE_CURRENT)
    }

    /**
     * @param unacknowledged will be accessed from different threads, so make sure wherever it's
     * *                       accessed it will be in a `synchronized(unacknowledged)
     * *                       {}` block
     */
    fun multiNotification(unacknowledged: Collection<Plaintext>, numberOfUnacknowledgedMessages: Int): NewMessageNotification {
        val builder = NotificationCompat.Builder(ctx)
        builder.setSmallIcon(R.drawable.ic_notification_new_message)
                .setContentTitle(ctx.getString(R.string.n_new_messages, numberOfUnacknowledgedMessages))
                .setContentText(ctx.getString(R.string.app_name))

        val inboxStyle = InboxStyle()

        synchronized(unacknowledged) {
            for (msg in unacknowledged) {
                val sb = SpannableString(msg.from.toString() + " " + msg.subject)
                sb.setSpan(SPAN_EMPHASIS, 0, msg.from.toString().length, Spannable
                        .SPAN_INCLUSIVE_EXCLUSIVE)
                inboxStyle.addLine(sb)
            }
        }
        builder.setStyle(inboxStyle)

        val intent = Intent(ctx, MainActivity::class.java)
        intent.action = MainActivity.ACTION_SHOW_INBOX
        val pendingIntent = PendingIntent.getActivity(ctx, 1, intent, 0)
        builder.setContentIntent(pendingIntent)
        notification = builder.build()
        return this
    }

    override val notificationId = NEW_MESSAGE_NOTIFICATION_ID

    companion object {
        private val NEW_MESSAGE_NOTIFICATION_ID = 1
        private val SPAN_EMPHASIS = StyleSpan(Typeface.BOLD)
    }
}
