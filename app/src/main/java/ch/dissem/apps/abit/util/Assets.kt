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

package ch.dissem.apps.abit.util

import android.content.Context
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import ch.dissem.apps.abit.R
import ch.dissem.bitmessage.entity.Plaintext
import java.io.IOException
import java.util.*

/**
 * Helper class to work with Assets.
 */
object Assets {
    fun readSqlStatements(ctx: Context, name: String): List<String> {
        try {
            val `in` = ctx.assets.open(name)
            val scanner = Scanner(`in`, "UTF-8").useDelimiter(";")
            val result = LinkedList<String>()
            while (scanner.hasNext()) {
                val statement = scanner.next().trim { it <= ' ' }
                if ("" != statement) {
                    result.add(statement)
                }
            }
            return result
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}

fun Plaintext.Status.getDrawable() = when (this) {
    Plaintext.Status.RECEIVED -> 0
    Plaintext.Status.DRAFT -> R.drawable.draft
    Plaintext.Status.PUBKEY_REQUESTED -> R.drawable.public_key
    Plaintext.Status.DOING_PROOF_OF_WORK -> R.drawable.ic_notification_proof_of_work
    Plaintext.Status.SENT -> R.drawable.sent
    Plaintext.Status.SENT_ACKNOWLEDGED -> R.drawable.sent_acknowledged
    else -> 0
}

fun Plaintext.Status.getString() = when (this) {
    Plaintext.Status.RECEIVED -> R.string.status_received
    Plaintext.Status.DRAFT -> R.string.status_draft
    Plaintext.Status.PUBKEY_REQUESTED -> R.string.status_public_key
    Plaintext.Status.DOING_PROOF_OF_WORK -> R.string.proof_of_work_title
    Plaintext.Status.SENT -> R.string.status_sent
    Plaintext.Status.SENT_ACKNOWLEDGED -> R.string.status_sent_acknowledged
    else -> 0
}
