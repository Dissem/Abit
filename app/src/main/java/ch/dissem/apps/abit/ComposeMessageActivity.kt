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

package ch.dissem.apps.abit

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import ch.dissem.apps.abit.service.Singleton
import ch.dissem.bitmessage.entity.Plaintext
import ch.dissem.bitmessage.entity.Plaintext.Encoding.EXTENDED
import kotlinx.android.synthetic.main.toolbar_layout.*

/**
 * Compose a new message.
 */
class ComposeMessageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.toolbar_layout)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setHomeAsUpIndicator(R.drawable.ic_action_close)
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(false)
        }

        if (supportFragmentManager.findFragmentById(R.id.content) == null) {
            // Display the fragment as the main content.
            val fragment = ComposeMessageFragment()
            fragment.arguments = intent.extras
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.content, fragment)
                .commit()
        }
    }

    companion object {
        const val EXTRA_DRAFT = "ch.dissem.abit.Message.DRAFT"
        const val EXTRA_IDENTITY = "ch.dissem.abit.Message.SENDER"
        const val EXTRA_RECIPIENT = "ch.dissem.abit.Message.RECIPIENT"
        const val EXTRA_SUBJECT = "ch.dissem.abit.Message.SUBJECT"
        const val EXTRA_CONTENT = "ch.dissem.abit.Message.CONTENT"
        const val EXTRA_BROADCAST = "ch.dissem.abit.Message.IS_BROADCAST"
        const val EXTRA_ENCODING = "ch.dissem.abit.Message.ENCODING"
        const val EXTRA_PARENT = "ch.dissem.abit.Message.PARENT"

        fun launchReplyTo(fragment: Fragment, item: Plaintext) =
            fragment.startActivity(
                getReplyIntent(
                    ctx = fragment.activity
                        ?: throw IllegalStateException("Fragment not attached to an activity"),
                    item = item
                )
            )

        fun launchReplyTo(activity: Activity, item: Plaintext) =
            activity.startActivity(getReplyIntent(activity, item))

        private fun getReplyIntent(ctx: Context, item: Plaintext): Intent {
            val replyIntent = Intent(ctx, ComposeMessageActivity::class.java)
            val receivingIdentity = item.to
            if (receivingIdentity?.isChan == true) {
                // reply to chan, not to the sender of the message
                replyIntent.putExtra(EXTRA_RECIPIENT, receivingIdentity)
                // I hate when people send as chan, so it won't be the default behaviour.
                replyIntent.putExtra(EXTRA_IDENTITY, Singleton.getIdentity(ctx))
            } else {
                replyIntent.putExtra(EXTRA_RECIPIENT, item.from)
                replyIntent.putExtra(EXTRA_IDENTITY, receivingIdentity)
            }
            // if the original message was sent using extended encoding, use it as well
            // so features like threading can be supported
            if (item.encoding == EXTENDED) {
                replyIntent.putExtra(EXTRA_ENCODING, EXTENDED)
            }
            replyIntent.putExtra(EXTRA_PARENT, item)
            item.subject?.let { subject ->
                val prefix: String = if (subject.length >= 3 && subject.substring(0, 3).equals(
                        "RE:",
                        ignoreCase = true
                    )
                ) {
                    ""
                } else {
                    "RE: "
                }
                replyIntent.putExtra(EXTRA_SUBJECT, prefix + subject)
            }
            replyIntent.putExtra(
                EXTRA_CONTENT,
                "\n\n------------------------------------------------------\n${item.text ?: ""}"
            )
            return replyIntent
        }
    }
}
