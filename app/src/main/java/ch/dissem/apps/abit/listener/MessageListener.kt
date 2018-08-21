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

package ch.dissem.apps.abit.listener

import android.content.Context
import ch.dissem.apps.abit.MainActivity
import ch.dissem.apps.abit.notification.NewMessageNotification
import ch.dissem.apps.abit.util.preferences
import ch.dissem.bitmessage.BitmessageContext
import ch.dissem.bitmessage.entity.Plaintext
import ch.dissem.bitmessage.ports.MessageRepository
import ch.dissem.bitmessage.utils.ConversationService
import java.util.*
import java.util.concurrent.Executors

/**
 * Listens for decrypted Bitmessage messages. Does show a notification.
 *
 *
 * Should show a notification when the app isn't running, but update the message list when it is.
 * Also,
 * notifications should be combined.
 *
 */
class MessageListener(ctx: Context) : BitmessageContext.Listener.WithContext {
    override fun setContext(ctx: BitmessageContext) {
        messageRepo = ctx.messages
        conversationService = ConversationService(messageRepo)
    }

    private val unacknowledged = LinkedList<Plaintext>()
    private var numberOfUnacknowledgedMessages = 0
    private val notification = NewMessageNotification(ctx)
    private val pool = Executors.newSingleThreadExecutor()
    private lateinit var messageRepo: MessageRepository
    private lateinit var conversationService: ConversationService

    init {
        emulateConversations = ctx.preferences.emulateConversations
    }

    override fun receive(plaintext: Plaintext) {
        pool.submit {
            updateConversation(plaintext)
            unacknowledged.addFirst(plaintext)
            numberOfUnacknowledgedMessages++
            if (unacknowledged.size > 5) {
                unacknowledged.removeLast()
            }
            if (numberOfUnacknowledgedMessages == 1) {
                notification.singleNotification(plaintext)
            } else {
                notification.multiNotification(unacknowledged, numberOfUnacknowledgedMessages)
            }
            notification.show()

            // If MainActivity is shown, update the sidebar badges
            MainActivity.apply { updateUnread() }
        }
    }

    fun resetNotification() {
        pool.submit {
            notification.hide()
            unacknowledged.clear()
            numberOfUnacknowledgedMessages = 0
        }
    }

    private fun updateConversation(plaintext: Plaintext) {
        if (emulateConversations && plaintext.encoding != Plaintext.Encoding.EXTENDED) {
            conversationService.getSubject(listOf(plaintext))?.let { subject ->
                plaintext.conversationId = UUID.nameUUIDFromBytes(subject.toByteArray())
                messageRepo.save(plaintext)
            }
        }
    }

    companion object {
        private var emulateConversations = false
    }
}
